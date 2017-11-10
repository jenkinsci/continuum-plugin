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
import jenkins.plugins.continuum.steps.CtmInitiatePipelineStep.CtmInitiatePipelineStepExecution;

public class TestCtmInitiatePipelineStep {

	@Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void buildWithEmptyServerUrlMustFail() throws Exception {
        WorkflowJob p = jenkins.jenkins.createProject(WorkflowJob.class, "p");
        
        p.setDefinition(new CpsFlowDefinition(
                "ctmInitiatePipeline serverUrl: '', project: 'prj1234', definition: 'def12324', group: 'grp1234', markUnstable: true"
        ));
        WorkflowRun b1 = p.scheduleBuild2(0).waitForStart();
        jenkins.assertBuildStatus(Result.UNSTABLE, jenkins.waitForCompletion(b1));
        jenkins.assertLogContains(CtmInitiatePipelineStepExecution.LOG_MESSAGE_INVALID_URL, b1);
    }

    @Test
    public void buildWithEmptyProjectMustFail() throws Exception {
        WorkflowJob p = jenkins.jenkins.createProject(WorkflowJob.class, "p");
        
        p.setDefinition(new CpsFlowDefinition(
                "ctmInitiatePipeline serverUrl: 'http://server.url', project: '', definition: 'def12324', group: 'grp1234', markUnstable: true"
        ));
        WorkflowRun b1 = p.scheduleBuild2(0).waitForStart();
        jenkins.assertBuildStatus(Result.UNSTABLE, jenkins.waitForCompletion(b1));
        jenkins.assertLogContains(CtmInitiatePipelineStepExecution.LOG_MESSAGE_INVALID_PROJECT, b1);
    }

    @Test
    public void buildWithEmptyGroupMustFail() throws Exception {
        WorkflowJob p = jenkins.jenkins.createProject(WorkflowJob.class, "p");
        
        p.setDefinition(new CpsFlowDefinition(
                "ctmInitiatePipeline serverUrl: 'http://server.url', project: 'prj1234', definition: 'def12324', group: '', markUnstable: true"
        ));
        WorkflowRun b1 = p.scheduleBuild2(0).waitForStart();
        jenkins.assertBuildStatus(Result.UNSTABLE, jenkins.waitForCompletion(b1));
        jenkins.assertLogContains(CtmInitiatePipelineStepExecution.LOG_MESSAGE_INVALID_GROUP, b1);
    }

    @Test
    public void buildWithEmptyDefinitionMustFail() throws Exception {
        WorkflowJob p = jenkins.jenkins.createProject(WorkflowJob.class, "p");
        
        p.setDefinition(new CpsFlowDefinition(
                "ctmInitiatePipeline serverUrl: 'http://server.url', project: 'prj1234', definition: '', group: 'grp1234', markUnstable: true"
        ));
        WorkflowRun b1 = p.scheduleBuild2(0).waitForStart();
        jenkins.assertBuildStatus(Result.UNSTABLE, jenkins.waitForCompletion(b1));
        jenkins.assertLogContains(CtmInitiatePipelineStepExecution.LOG_MESSAGE_INVALID_DEFINITION, b1);
    }
}
