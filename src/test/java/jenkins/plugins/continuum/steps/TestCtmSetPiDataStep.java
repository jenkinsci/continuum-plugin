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

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.model.Result;
import jenkins.plugins.continuum.steps.CtmPostPiDataStep.CtmPostPiDataStepExecution;

public class TestCtmSetPiDataStep {

	@Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void buildWithEmptyServerUrlMustFail() throws Exception {
        WorkflowJob p = jenkins.jenkins.createProject(WorkflowJob.class, "p");
        
        p.setDefinition(new CpsFlowDefinition(
                "ctmSetPiData serverUrl: '', key: 'key1234', value: 'val12324', markUnstable: true"
        ));
        WorkflowRun b1 = p.scheduleBuild2(0).waitForStart();
        jenkins.assertBuildStatus(Result.UNSTABLE, jenkins.waitForCompletion(b1));
        jenkins.assertLogContains(CtmPostPiDataStepExecution.LOG_MESSAGE_INVALID_URL, b1);
    }

    @Test
    public void buildWithEmptyKeyMustFail() throws Exception {
        WorkflowJob p = jenkins.jenkins.createProject(WorkflowJob.class, "p");
        
        p.setDefinition(new CpsFlowDefinition(
                "ctmSetPiData serverUrl: 'http://server.url', key: '', value: 'val12324', markUnstable: true"
        ));
        WorkflowRun b1 = p.scheduleBuild2(0).waitForStart();
        jenkins.assertBuildStatus(Result.UNSTABLE, jenkins.waitForCompletion(b1));
        jenkins.assertLogContains(CtmPostPiDataStepExecution.LOG_MESSAGE_INVALID_KEY, b1);
    }

    @Test
    public void buildWithEmptyValueMustFail() throws Exception {
        WorkflowJob p = jenkins.jenkins.createProject(WorkflowJob.class, "p");
        
        p.setDefinition(new CpsFlowDefinition(
                "ctmSetPiData serverUrl: 'http://server.url', key: 'key1234', value: '', markUnstable: true"
        ));
        WorkflowRun b1 = p.scheduleBuild2(0).waitForStart();
        jenkins.assertBuildStatus(Result.UNSTABLE, jenkins.waitForCompletion(b1));
        jenkins.assertLogContains(CtmPostPiDataStepExecution.LOG_MESSAGE_INVALID_VALUE, b1);
    }

}
