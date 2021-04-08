package com.autodesk.config;

import com.autodesk.BuildTimeoutListener;
import hudson.Functions;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.JobProperty;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.queue.QueueTaskFuture;
import hudson.tasks.BatchFile;
import hudson.tasks.Shell;
import jenkins.model.CauseOfInterruption;
import jenkins.model.InterruptedBuildAction;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;
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
        for (int i=0; i<4; i++) {
            jenkinsRule.createOnlineSlave();
        }
        // Setting up freestyle project
        freeStyleProject = jenkinsRule.createProject(FreeStyleProject.class, "freestyle");
        // Setting up pipeline project
        pipelineProject = jenkinsRule.createProject(WorkflowJob.class, "pipeline");
        LOGGER.info("Setup method complete!");
    }

    @After
    public void teardown() throws IOException {
        List<JobProperty<? super FreeStyleProject>> allProperties = freeStyleProject.getAllProperties();
        for (JobProperty<? super FreeStyleProject> property : allProperties) {
            freeStyleProject.removeProperty(property);
        }
        List<JobProperty<? super WorkflowJob>> properties = pipelineProject.getAllProperties();
        for (JobProperty<? super WorkflowJob> property : properties) {
            pipelineProject.removeProperty(property);
        }
    }

    // With global timeout and with both global and job timeout. All pass within timeout
    @Test
    public void testWithinTime() throws Exception {

        LOGGER.info("Running integration test for job completion within timeout");
        // Testing freestyle without job timeout
        GlobalTimeoutConfig.get().setBuildTimeout(1);  // setting timeout of 5 minutes. more than enough to complete
        setFreestyleDuration(1);
        FreeStyleBuild freeStyleBuild = freeStyleProject.scheduleBuild2(0).get();
        jenkinsRule.assertBuildStatus(Result.SUCCESS, freeStyleBuild);
        LOGGER.info("Freestyle build complete... Testing pipeline now");
        // Testing pipeline without job timeout
        setPipelineDurationWithoutJobTimeout(1);
        WorkflowRun workflowRun = pipelineProject.scheduleBuild2(0).get();
        jenkinsRule.assertBuildStatus(Result.SUCCESS, workflowRun);

        // Testing with both job timeout and global timeout
        freeStyleProject.addProperty(new JobTimeoutProperty(1));
        FreeStyleBuild freeStyleBuildBoth = freeStyleProject.scheduleBuild2(0).get();
        jenkinsRule.assertBuildStatus(Result.SUCCESS, freeStyleBuildBoth);
        setPipelineWithJobTimeout(1,1);
        WorkflowRun workflowRunBoth = Objects.requireNonNull(pipelineProject.scheduleBuild2(0)).get();

        jenkinsRule.assertBuildStatus(Result.SUCCESS, workflowRunBoth);
    }

    // Only global timeout set. Exceed global timeout
    @Test
    public void testExceedGlobalTimeout() throws Exception {

        GlobalTimeoutConfig.get().setBuildTimeout(1);
        // Starting builds
        setFreestyleDuration(71);
        QueueTaskFuture<FreeStyleBuild> freeStyleBuildQueueTaskFuture = freeStyleProject.scheduleBuild2(0);
        setPipelineDurationWithoutJobTimeout(71);
        QueueTaskFuture<WorkflowRun> workflowRunQueueTaskFuture = pipelineProject.scheduleBuild2(0);

        // Asserting results
        FreeStyleBuild freeStyleBuild = freeStyleBuildQueueTaskFuture.get();
        assertBuildStoppedByJobTimeout(freeStyleBuild);
        WorkflowRun workflowRun = workflowRunQueueTaskFuture.get();
        assertBuildStoppedByJobTimeout(workflowRun);
    }

    // Global timeout set with overriding job timeout. Build pass within job timeout despite exceeding global timeout
    @Test
    public void testJobTimeoutPass() throws Exception {

        GlobalTimeoutConfig.get().setBuildTimeout(1);
        // Starting freestyle
        freeStyleProject.addProperty(new JobTimeoutProperty(2));
        setFreestyleDuration(65);
        QueueTaskFuture<FreeStyleBuild> freeStyleBuildQueueTaskFuture = freeStyleProject.scheduleBuild2(0);
        // Starting pipeline
        setPipelineWithJobTimeout(1, 2);
        pipelineProject.scheduleBuild2(0).get();
        setPipelineWithJobTimeout(61, 2);
        QueueTaskFuture<WorkflowRun> workflowRunQueueTaskFuture = pipelineProject.scheduleBuild2(0);
        // Asserting results
        FreeStyleBuild freeStyleBuild = freeStyleBuildQueueTaskFuture.get();
        jenkinsRule.assertBuildStatus(Result.SUCCESS, freeStyleBuild);
        WorkflowRun workflowRun = workflowRunQueueTaskFuture.get();
        jenkinsRule.assertBuildStatus(Result.SUCCESS, workflowRun);
    }

    // Testing job timeout abort due to JobTimeoutProperty set without a global timeout
    @Test
    public void testJobTimeout() throws Exception {

        GlobalTimeoutConfig.get().setBuildTimeout(0);  // setting no global timeout
        // Starting freestyle
        LOGGER.info("Starting freestyle job");
        freeStyleProject.addProperty(new JobTimeoutProperty(1));
        setFreestyleDuration(61);
        QueueTaskFuture<FreeStyleBuild> freeStyleBuildQueueTaskFuture = freeStyleProject.scheduleBuild2(0);
        // Starting pipeline
        LOGGER.info("Starting pipeline job");
        setPipelineWithJobTimeout(10, 1);
        pipelineProject.scheduleBuild2(0).get();
        setPipelineWithJobTimeout(70, 1);
        QueueTaskFuture<WorkflowRun> workflowRunQueueTaskFuture = pipelineProject.scheduleBuild2(0);

        // checking results
        LOGGER.info("Checking freestyle build result. Expecting an abort");
        FreeStyleBuild freeStyleBuild = freeStyleBuildQueueTaskFuture.get();
        assertBuildStoppedByJobTimeout(freeStyleBuild);
        LOGGER.info("Checking workflow run result. Expecting an abort");
        WorkflowRun workflowRun = workflowRunQueueTaskFuture.get();
        assertBuildStoppedByJobTimeout(workflowRun);
    }

    // Infinite loop, test if can interrupt
    @Test
    public void testPipelineInfiniteLoop() throws Exception {

        WorkflowJob infiniteLoop = jenkinsRule.createProject(WorkflowJob.class, "infiniteLoop");
        GlobalTimeoutConfig.get().setBuildTimeout(1);
        String cmd;
        if (Functions.isWindows()) {
            cmd = "        bat 'echo in loop; timeout 10'\n";
        } else {
            cmd = "        sh 'echo in loop; sleep 10'\n";
        }
        LOGGER.info("Starting a run that sets the job timeout property");
        String jenkinsfileString = "node() {\n"
                + "    while(1) {\n"
                + cmd
                + "    }\n"
                + "}";
        infiniteLoop.setDefinition(new CpsFlowDefinition(jenkinsfileString, false));
        WorkflowRun workflowRun = Objects.requireNonNull(infiniteLoop.scheduleBuild2(0)).get();
        jenkinsRule.assertBuildStatus(Result.ABORTED, workflowRun);

    }

    private void assertBuildStoppedByJobTimeout(Run build) throws Exception {

        jenkinsRule.assertBuildStatus(Result.ABORTED, build);
        List<InterruptedBuildAction> actions = build.getActions(InterruptedBuildAction.class);
        assertEquals(1, actions.size());
        InterruptedBuildAction interruptedBuildAction = actions.get(0);
        List<CauseOfInterruption> causeOfInterruptions = interruptedBuildAction.getCauses();
        assertEquals(1, causeOfInterruptions.size());
        assertTrue(causeOfInterruptions.get(0) instanceof BuildTimeoutListener.JobTimeoutInterruption);
    }

    private void setPipelineDurationWithoutJobTimeout(int durationSeconds) {
        String cmd;
        if (Functions.isWindows()) {
            cmd = "bat 'timeout " + durationSeconds + "; echo done'";
        } else {
            cmd = "sh 'sleep " + durationSeconds + "; echo done'";
        }
        String jenkinsfileString = "node() { " + cmd + " }";
        pipelineProject.setDefinition(new CpsFlowDefinition(jenkinsfileString, false));
    }

    private void setPipelineWithJobTimeout(int durationSeconds, int jobTimeoutMinutes) {
        String jenkinsfileString = "properties([jobTimeoutProperty(" + jobTimeoutMinutes + ")])\n"
                + "node() {\n"
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
