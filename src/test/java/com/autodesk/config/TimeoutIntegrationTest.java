package com.autodesk.config;

import hudson.Functions;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import hudson.model.Result;
import hudson.model.labels.LabelAtom;
import hudson.slaves.DumbSlave;
import hudson.tasks.BatchFile;
import hudson.tasks.Shell;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * Integration tests
 */
public class TimeoutIntegrationTest {

    @Rule
    public final JenkinsRule jenkinsRule = new JenkinsRule();
    Logger LOGGER = Logger.getLogger(TimeoutIntegrationTest.class.getName());
    FreeStyleProject freeStyleProject;
    WorkflowJob pipelineProject;


    @Before
    public void setup() throws Exception {

        LOGGER.info("Running setup method");
        Label testLabel = new LabelAtom("test-label");
        DumbSlave testAgent = jenkinsRule.createOnlineSlave(testLabel);
        // Setting up freestyle project
        freeStyleProject = jenkinsRule.createProject(FreeStyleProject.class, "freestyle");
        freeStyleProject.setAssignedLabel(testLabel);
        // Setting up pipeline project
        pipelineProject = jenkinsRule.createProject(WorkflowJob.class, "pipeline");
        LOGGER.info("Setup method complete!");
    }

    @Test
    public void testWithinTime() throws Exception {

        LOGGER.info("Running integration test for job completion within timeout");
        // Testing freestyle
        GlobalTimeoutConfig.get().setBuildTimeout(1);  // setting timeout of 5 minutes. more than enough to complete
        setFreestyleDuration(1);
        FreeStyleBuild freeStyleBuild = freeStyleProject.scheduleBuild2(0).get();
        jenkinsRule.assertBuildStatus(Result.SUCCESS, freeStyleBuild);
        LOGGER.info("Freestyle build complete... Testing pipeline now");
        // Testing pipeline
        setPipelineDuration(1);
        WorkflowRun workflowRun = pipelineProject.scheduleBuild2(0).get();
        jenkinsRule.assertBuildStatus(Result.SUCCESS, workflowRun);
    }

    @Test
    public void testExceedTimeout() throws Exception {

        // Testing freestyle
        GlobalTimeoutConfig.get().setBuildTimeout(1);
        setFreestyleDuration(61);
        FreeStyleBuild freeStyleBuild = freeStyleProject.scheduleBuild2(0).get();
        jenkinsRule.assertBuildStatus(Result.ABORTED, freeStyleBuild);
        // Testing pipeline
        setPipelineDuration(61);
        WorkflowRun workflowRun = Objects.requireNonNull(pipelineProject.scheduleBuild2(0)).get();
        jenkinsRule.assertBuildStatus(Result.ABORTED, workflowRun);
    }

    private void setPipelineDuration(int durationSeconds) {
        String jenkinsfileString = "node() { sh 'sleep " + durationSeconds + "; echo done'}";
        pipelineProject.setDefinition(new CpsFlowDefinition(jenkinsfileString, false));
    }

    private void setFreestyleDuration(int durationSeconds) {
        if (Functions.isWindows()) {
            freeStyleProject.getBuildersList().clear();
            freeStyleProject.getBuildersList().add(new BatchFile("timeout /t " + durationSeconds
                    + "\necho done"));
        } else {
            freeStyleProject.getBuildersList().clear();
            freeStyleProject.getBuildersList().add(new Shell("sleep " + durationSeconds + "s;echo done"));
        }
    }
}
