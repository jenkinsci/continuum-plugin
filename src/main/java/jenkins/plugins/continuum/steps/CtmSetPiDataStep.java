/*
 * Copyright 2017 CollabNet, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jenkins.plugins.continuum.steps;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.util.FormValidation;
import jenkins.plugins.continuum.ContinuumConstants;
import jenkins.plugins.continuum.PostPiDataInputBuilder;
import jenkins.plugins.continuum.actions.PipelineInitiatedAction;
import jenkins.plugins.continuum.steps.CtmPostPiDataStep.CtmPostPiDataStepExecution;
import net.sf.json.JSONObject;

public class CtmSetPiDataStep extends CtmCommandStep {

    /** The continuum pipeline identifier */
    @DataBoundSetter public String pi;

    /** The workspace data key */
    @DataBoundSetter public String key;

    /** The workspace data value. */
    @DataBoundSetter public String value;

    /** Whether to post the data to the last initiated pipeline */
    @DataBoundSetter public boolean useLastPi;

    @DataBoundConstructor
    public CtmSetPiDataStep(String serverUrl) {
        super(serverUrl);
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        CtmSetPiDataStepExecution execution = new CtmSetPiDataStepExecution(this, context);
        return execution;
    }

    @Extension
    public static class DescriptorImpl extends CtmCommandStepDescriptor {

        @Override
        public String getFunctionName() {
            return "ctmSetPiData";
        }

        @Override
        public String getDisplayName() {
            return "Set workspace data on a running Continuum pipeline instance.";
        }

        /**
         * Validates that the user provided a data key.
         *
         * @param key
         *            the workspace data key
         * @return whether or not the validation succeeded
         */
        public FormValidation doCheckKey(@QueryParameter String key) {
            return FormValidation.validateRequired(key);
        }

        /**
         * Validates that the user provided a value for the specified key.
         *
         * @param value
         *            the data value provided by the user
         * @return whether or not the validation succeeded
         */
        public FormValidation doCheckValue(@QueryParameter String value) {
            return FormValidation.validateRequired(value);
        }
    }

    public static class CtmSetPiDataStepExecution extends CtmCommandStepExecution<CtmSetPiDataStep> {
        private static final long serialVersionUID = 1L;
        private static final Logger logger = Logger.getLogger(CtmSetPiDataStepExecution.class.getName());

        public CtmSetPiDataStepExecution(final CtmSetPiDataStep step, @Nonnull final StepContext ctx)
                throws IOException, InterruptedException {
            super(step, ctx);
        }

        @Override
        protected String getCommandName() {
            return ContinuumConstants.COMMAND_NAME__SET_PI_DATA;
        }

        @Override
        protected Void run() throws Exception {
            PrintStream consoleLogger = this.listener.getLogger();
            String serverUrl = getServerUrl();
            if (isBlank(serverUrl)) {
                markUnstable(consoleLogger, LOG_MESSAGE_INVALID_URL);
                return null;
            }

            String key = this.step.key;
            if (isBlank(key)) {
                markUnstable(consoleLogger, CtmPostPiDataStepExecution.LOG_MESSAGE_INVALID_KEY);
                return null;
            }

            String value = this.step.value;
            if (isBlank(value)) {
                markUnstable(consoleLogger, CtmPostPiDataStepExecution.LOG_MESSAGE_INVALID_VALUE);
                return null;
            }

            try {
                initialize();
                Set<String> pis = getPipelineIds();
                if (pis == null || pis.isEmpty()) {
                    log("There are no pipelines to set data on", consoleLogger);
                    return null;
                }
                for (String pi : pis) {
                    Map<String, Object> commandParams = new HashMap<String, Object>();
                    commandParams.put(ContinuumConstants.COMMAND_PARAMETER__PI, pi);
                    commandParams.put(ContinuumConstants.COMMAND_PARAMETER__KEY, key);
                    Object valueObj = null;
                    try {
                        valueObj = JSONObject.fromObject(value);
                    }
                    catch (Exception e) {
                        valueObj = value;
                    }
                    commandParams.put(ContinuumConstants.COMMAND_PARAMETER__VALUE, valueObj);
                    String responseString = executeCommand(commandParams);
                    log("Pipeline data set: " + responseString, consoleLogger);
                }

            } catch (IllegalStateException ise) {
                markUnstable(consoleLogger,
                        "this step needs a Jenkins URL " +
                        "(go to Manage Jenkins > Configure System; click Save)");
                ise.printStackTrace(consoleLogger);
            } catch (Exception e) {
                markUnstable(consoleLogger, e.getMessage());
                log("Failed to set PI data...Details: ", consoleLogger);
                e.printStackTrace(consoleLogger);
            }
            return null;
        }

        private Set<String> getPipelineIds() {
            Set<String> result = new HashSet<String>();
            if (!isBlank(this.step.pi)) {
                String[] pis = this.step.pi.trim().split("\\s*,\\s*");
                if (pis.length > 0) {
                    result.addAll(Arrays.asList(pis));
                }
            }
            else {
                PipelineInitiatedAction pia = this.run.getAction(PipelineInitiatedAction.class);
                if (pia != null) {
                	String serverUrl = getServerUrl();
                    if (this.step.useLastPi) {
                        String lastPi = pia.getLastPipelineId(serverUrl);
                        if (!isBlank(lastPi)) {
                            result.add(lastPi);
                        }
                    }
                    else {
                        result.addAll(pia.getPipelineIds(serverUrl));
                    }
                }
            }
            return result;
        }

        /**
         * Marks the current run as unstable and logs a message.
         * 
         * @param consoleLogger
         *            the logger to log to
         * @param message
         *            the message to log
         */
        private void markUnstable(PrintStream consoleLogger, String message) {
            log(message, consoleLogger);
            logger.warning(message);
            markUnstable();
        }
        
        /**
         * Jenkins (un)helpfully wipes out any initialization done in constructors
         * or class definitions before executing this #perform method. So we need to
         * initialize it in case it wasn't already.
         */
        private void initialize() {
            if (this.converter == null) {
            	// Use the same converter as post_pi_data
                this.converter = new PostPiDataInputBuilder();
            }
        }

    }
}
