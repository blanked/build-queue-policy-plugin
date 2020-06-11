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
public class BuildTimeoutIntegrationTests {

    @Rule
    public final JenkinsRule jenkinsRule = new JenkinsRule();
    Logger LOGGER = Logger.getLogger(BuildTimeoutIntegrationTests.class.getName());
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

    // Testing override of global timeout with build timeout. Exceed global but within build timeout
    @Test
    public void testJobTimeoutFreestylePass() throws Exception {

        GlobalTimeoutConfig.get().setBuildTimeout(1);
        // Testing freestyle
        freeStyleProject.addProperty(new JobTimeoutProperty(2));
        setFreestyleDuration(61);
        FreeStyleBuild freeStyleBuild = freeStyleProject.scheduleBuild2(0).get();
        jenkinsRule.assertBuildStatus(Result.SUCCESS, freeStyleBuild);
        freeStyleProject.removeProperty(JobTimeoutProperty.class);
    }

    @Test
    public void testJobTimeoutPipelinePass() throws Exception {

        // Testing pipeline
        setPipelineWithJobTimeout(61, 2);
        WorkflowRun workflowRun = Objects.requireNonNull(pipelineProject.scheduleBuild2(0)).get();
        String log = workflowRun.getLog();
        jenkinsRule.assertBuildStatus(Result.SUCCESS, workflowRun);
    }

    @Test
    public void testFreestyleJobTimeout() throws Exception {

        GlobalTimeoutConfig.get().setBuildTimeout(0);
        // Testing freestyle
        LOGGER.info("Testing freestyle");
        freeStyleProject.addProperty(new JobTimeoutProperty(1));
        setFreestyleDuration(61);
        FreeStyleBuild freeStyleBuild = freeStyleProject.scheduleBuild2(0).get();
        jenkinsRule.assertBuildStatus(Result.ABORTED, freeStyleBuild);
        freeStyleProject.removeProperty(JobTimeoutProperty.class);
        LOGGER.info("Freestyle completed successfully. Testing pipeline");
    }

    @Test
    public void testPipelineJobTimeout() throws Exception {

        GlobalTimeoutConfig.get().setBuildTimeout(0);
        LOGGER.info("Testing pipeline job timeout");
        setPipelineWithJobTimeout(61, 1);
        WorkflowRun workflowRun = Objects.requireNonNull(pipelineProject.scheduleBuild2(0)).get();
        jenkinsRule.assertBuildStatus(Result.ABORTED, workflowRun);
    }

    private void setPipelineDuration(int durationSeconds) {
        String jenkinsfileString = "node() { sh 'sleep " + durationSeconds + "; echo done'}";
        pipelineProject.setDefinition(new CpsFlowDefinition(jenkinsfileString, false));
    }

    private void setPipelineWithJobTimeout(int durationSeconds, int jobTimeoutMinutes) {
        String jenkinsfileString = "node() {\n"
                + "    jobTimeoutProperty(2)\n"
                + "    sh 'sleep " + durationSeconds + "; echo done'\n"
                + "}";
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