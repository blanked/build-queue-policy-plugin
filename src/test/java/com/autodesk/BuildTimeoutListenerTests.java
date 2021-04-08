package com.autodesk;

import hudson.Functions;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.queue.QueueTaskFuture;
import hudson.tasks.BatchFile;
import hudson.tasks.Shell;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

/**
 * Test class for {@link BuildTimeoutListener}
 */
public class BuildTimeoutListenerTests {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();
    Logger LOGGER = Logger.getLogger(BuildTimeoutListenerTests.class.getName());

    @Before
    public void setup() throws Exception {
        jenkinsRule.createOnlineSlave();
        jenkinsRule.createOnlineSlave();
    }

    @Test
    public void testAbortFreeStyle() throws Exception {
        // Starting freestyle
        FreeStyleProject freeStyleProject = jenkinsRule.createFreeStyleProject();
        if (Functions.isWindows()) {
            freeStyleProject.getBuildersList().add(new BatchFile("timeout 600; echo done"));
        } else {
            freeStyleProject.getBuildersList().add(new Shell("sleep 600;echo done"));
        }
        QueueTaskFuture<FreeStyleBuild> freeStyleBuildQueueTaskFuture = freeStyleProject.scheduleBuild2(0);
        FreeStyleBuild freeStyleBuild = freeStyleBuildQueueTaskFuture.waitForStart();
        while(freeStyleBuild.hasntStartedYet()) {
            Thread.sleep(1000);
        }
        // Aborting freestyle
        BuildTimeoutListener buildTimeoutListener = new BuildTimeoutListener();
        buildTimeoutListener.abortBuild(freeStyleBuild);
        jenkinsRule.assertBuildStatus(Result.ABORTED, freeStyleBuild);
}

    @Test
    public void testAbortPipeline() throws Exception {

        // Starting pipeline build
        WorkflowJob workflowJob = jenkinsRule.createProject(WorkflowJob.class);
        String jenkinsfileString;
        LOGGER.info("is windows: " + Functions.isWindows());
        if (Functions.isWindows()) {
            jenkinsfileString = "node() { bat('timeout 60; echo done') }";
        } else {
            jenkinsfileString = "node() { sh('sleep 60s; echo done') }";
        }
        workflowJob.setDefinition(new CpsFlowDefinition(jenkinsfileString, false));
        QueueTaskFuture<WorkflowRun> workflowRunQueueTaskFuture = workflowJob.scheduleBuild2(0);
        assert workflowRunQueueTaskFuture != null;
        WorkflowRun workflowRun = workflowRunQueueTaskFuture.waitForStart();
        while(workflowRun.hasntStartedYet()) {
            Thread.sleep(1000);
        }
        // Aborting pipeline run
        BuildTimeoutListener buildTimeoutListener = new BuildTimeoutListener();
        buildTimeoutListener.abortBuild(workflowRun);
        jenkinsRule.assertBuildStatus(Result.ABORTED, workflowRun);
    }

    @Test
    public void pipelineSucceed() throws Exception {
        WorkflowJob workflowJob = jenkinsRule.createProject(WorkflowJob.class);
        String jenkinsfileString;
        if (Functions.isWindows()) {
            jenkinsfileString = "node() { bat('echo done') }";
        } else {
            jenkinsfileString = "node() { sh('echo done') }";
        }
        LOGGER.info("jenkinsfileString: " + jenkinsfileString);
        workflowJob.setDefinition(new CpsFlowDefinition(jenkinsfileString, false));
        WorkflowRun workflowRun = Objects.requireNonNull(workflowJob.scheduleBuild2(0)).get();
        jenkinsRule.assertBuildStatus(Result.SUCCESS, workflowRun);
    }
}
