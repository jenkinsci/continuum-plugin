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
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.google.common.collect.ImmutableSet;

import hudson.EnvVars;
import hudson.model.Item;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.plugins.continuum.BuildToContinuumAPI;
import jenkins.plugins.continuum.ContinuumClient;
import jenkins.plugins.continuum.ContinuumConstants;
import jenkins.plugins.continuum.steps.CtmInitiatePipelineStep.CtmInitiatePipelineStepExecution;

/**
 * Base class for Continuum command steps
 */
public abstract class CtmCommandStep extends Step {

    /** The root URL to the Continuum server. */
    private final String ctmUrl;

    private String credentialsId;

    /** The flag to mark current run unstable if this step fails. */
    @DataBoundSetter public boolean markUnstable;

    /** The Continuum API token, use this parameter instead of selecting a 'Credential' */
    @DataBoundSetter public String apiToken;

    public CtmCommandStep(String serverUrl) {
        this.ctmUrl = serverUrl;
    }

    /**
     * Reads the root server URL to contact. Used by Jelly to render the UI template.
     *
     * @return the Continuum server base URL
     */
    public String getServerUrl() {
        return this.ctmUrl;
    }

    public String getCredentialsId() {
        return this.credentialsId;
    }

    @DataBoundSetter public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    /**
     * Base descriptor for Continuum command steps
     */
    public static abstract class CtmCommandStepDescriptor extends StepDescriptor {

        @Override
        public Set<Class<?>> getRequiredContext() {
            return ImmutableSet.of(Run.class, TaskListener.class, EnvVars.class);
        }

    	public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item owner) {
            if (owner == null || !owner.hasPermission(Item.CONFIGURE)) {
                return new StandardListBoxModel().withEmptySelection();
            }
            String useServerUrl = null;
            return new StandardListBoxModel()
                    .withEmptySelection()
                    .withAll(CtmInitiatePipelineStepExecution.lookupCredentials(owner, useServerUrl));
        }

        /**
         * Validates that the user provided a server URL.
         *
         * @param serverUrl
         *            the URL provided by the user
         * @return whether or not the validation succeeded
         */
        public FormValidation doCheckServerUrl(@QueryParameter String serverUrl) {
            return FormValidation.validateRequired(serverUrl);
        }

        public FormValidation doCheckCredentialsId(
                @QueryParameter final String credentialsId,
                @AncestorInPath final Item owner) {
            return FormValidation.ok();
        }
    }

    /**
     * Base execution implementation for Continuum steps
     * @param <T>
     */
    public static abstract class CtmCommandStepExecution<T extends CtmCommandStep> extends SynchNonBlockingStepExecution<Void> {
        private static final long serialVersionUID = 1L;

        /** Message for invalid Continuum server URL */
        public static String LOG_MESSAGE_INVALID_URL = "The URL to the Continuum server is missing.";

        protected transient T step;
        protected transient TaskListener listener;
        protected transient Run<?,?> run;
        protected transient BuildToContinuumAPI converter;
        
        public CtmCommandStepExecution(final T step, @Nonnull final StepContext ctx)
                throws IOException, InterruptedException {
            super(ctx);
            this.step = step;
            this.listener = getContext().get(TaskListener.class);
            this.run = getContext().get(Run.class);
        }

        protected String executeCommand(Map<String,Object> commandParameters) throws Exception {
        	String payload = converter.toContinuumAPI(run, commandParameters);
        	try {
        		String commandResult = ContinuumClient.post(getServerUrl(), getAPIToken(),
        				getCommandName(), payload);
        		return commandResult;
        	}
        	catch (Exception e) {
        		log("Request payload: " + payload, this.listener.getLogger());
        		throw e;
        	}
        }

        protected EnvVars getEnvVars() {
        	try {
        		return getContext().get(EnvVars.class);
        	} catch (InterruptedException e) {
        	} catch (IOException e) {
			}
    		return null;
        }

        protected String getServerUrl() {
        	String url = this.step.getServerUrl();
        	if (isBlank(url)) {
        		EnvVars env = getEnvVars();
        		if (env != null) {
        			url = env.get(ContinuumConstants.ENV_VARIABLE__SERVER_URL);
        		}
        	}
        	return url;
        }

        protected String getCredentialsId() {
        	String credId = this.step.getCredentialsId();
        	if (isBlank(credId)) {
        		EnvVars env = getEnvVars();
        		if (env != null) {
        			credId = env.get(ContinuumConstants.ENV_VARIABLE__CREDENTIAL_ID);
        		}
        	}
        	return credId;
        }

        protected String getAPIToken() {
            String token = this.step.apiToken;
            if (isBlank(token)) {
                StandardCredentials sc = getCredentials();
                if (sc instanceof StandardUsernamePasswordCredentials) {
                    Secret secret = ((StandardUsernamePasswordCredentials) sc).getPassword(); 
                    token = secret == null ? null : secret.getPlainText();
                }
                else {
                    try {
                        java.lang.reflect.Method m = sc.getClass().getMethod("getSecret");
                        Object obj = m.invoke(sc);
                        if (obj instanceof Secret) {
                            token = ((Secret) obj).getPlainText();
                        }
                    }
                    catch (Exception e) {
                        // ignore
                    }
                }
            }
            return token;
        }

        public StandardCredentials getCredentials() {
            return getCredentials(this.run.getParent(),
                    getCredentialsId(), getServerUrl());
        }
        
        /**
         * Marks the current run as unstable and logs a message.
         */
        protected void markUnstable() {
            if (this.step == null || this.step.markUnstable) {
                if (this.run != null) {
                    this.run.setResult(Result.UNSTABLE);
                }
            }
        }

        protected abstract String getCommandName();

        /**
         * Logging helper that prepends the log message prefix
         *
         * @param msg
         * @param printStream
         */
        protected void log(String msg, PrintStream printStream) {
            printStream.print("Continuum " + getCommandName() + " - ");
            printStream.println(msg);
        }

        public static StandardCredentials getCredentials(Item owner,
                String credentialsId, String serverUrl) {
            StandardCredentials result = null;
            if (!isBlank(credentialsId)) {
                for (StandardCredentials c : lookupCredentials(owner, serverUrl)) {
                    if (c.getId().equals(credentialsId)) {
                        result = c;
                        break;
                    }
                }
            }
            return result;
        }

        public static List<StandardCredentials> lookupCredentials(Item owner, String serverUrl) {
            URIRequirementBuilder rBuilder = isBlank(serverUrl) ?
                    URIRequirementBuilder.create() : URIRequirementBuilder.fromUri(serverUrl);
            return CredentialsProvider.lookupCredentials(
                    StandardCredentials.class, owner, null, rBuilder.build());
        }
    }
}
