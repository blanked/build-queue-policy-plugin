package com.autodesk;

import com.autodesk.config.GlobalTimeoutConfig;
import hudson.Functions;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import hudson.model.PeriodicWork;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.labels.LabelAtom;
import hudson.slaves.DumbSlave;
import jenkins.model.CauseOfInterruption;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import org.kohsuke.stapler.HttpResponse;
import org.mockito.Mockito;
import org.mockito.Spy;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Test suite for Queue Timeout using mock
 */
public class MockQueueTests {

    // TODO - mock a queue
    @Spy
    @Rule
    public JenkinsRule testJenkinsRule;
    @Spy
    @Rule
    public final JenkinsRule jenkinsRule = new JenkinsRule();
    @Spy
    Queue queue;
    private final Logger LOGGER = Logger.getLogger(QueueTimeout.class.getName());
    QueueTimeout queueTimeout;
    DumbSlave agent;

    @Before
    public void setup() throws Exception {
        // Getting a spy of the queue
        queue = spy(Queue.getInstance());
        if (agent == null) {
            agent = jenkinsRule.createOnlineSlave(new LabelAtom("valid-label"));  // creating a valid test label
        }
        this.queueTimeout = spy(PeriodicWork.all().get(QueueTimeout.class));
        // Consider using negative as no limit and 0 as no waiting time?

    }

    @After
    public void teardown() throws Exception {
        jenkinsRule.disconnectSlave(agent);
        Thread.sleep(TimeUnit.SECONDS.toMillis(3));
        agent = null;
    }

    // Test happy path
    @Test
    public void testPass() throws Exception {

        // Set timeout and build should pass even with the timeout in place
        GlobalTimeoutConfig.get().setQueueTimeout(1);
        GlobalTimeoutConfig.get().setNoSuchNodeQueueTimeout(1);

        // Testing pipeline
        WorkflowJob workflowJob = createWorkflowJob("valid-label");
        WorkflowRun workflowRun = workflowJob.scheduleBuild2(0).get();

        // verify that no queue task/item was cancelled
        verify(queue, never()).cancel(Mockito.any(Queue.Task.class));
        verify(queueTimeout, never()).submitStopQueueItemThread(any(Queue.Item.class), any(CauseOfInterruption.class));
        // verify that the build succeeded
        jenkinsRule.assertBuildStatus(Result.SUCCESS, workflowRun);

        // Testing freestyle
        FreeStyleProject freeStyleProject = jenkinsRule.createProject(FreeStyleProject.class, "freestyle-project");
        freeStyleProject.setAssignedLabel(new LabelAtom("valid-label"));
        FreeStyleBuild freeStyleBuild = freeStyleProject.scheduleBuild2(0).waitForStart();
        // verify that no queue task/item was cancelled
        verify(queue, never()).cancel(Mockito.any(Queue.Task.class));
        verify(queueTimeout, never()).submitStopQueueItemThread(any(Queue.Item.class), any(CauseOfInterruption.class));
    }

    @Test
    public void testPipelineInvalidNode() throws Exception {

        // Setting invalid node timeout of 1 minute
        GlobalTimeoutConfig.get().setNoSuchNodeQueueTimeout(1);
        // Creating the job
        LOGGER.info("Creating a workflow job");
        WorkflowJob workflowJob = createWorkflowJob("invalid-label");
        LOGGER.info("Scheduling a build");
        WorkflowRun workflowRun = workflowJob.scheduleBuild2(0).waitForStart();
        // wait 1 minute and 10 seconds for the timeout to be exceeded
        Thread.sleep(TimeUnit.MINUTES.toMillis(1) + TimeUnit.SECONDS.toMillis(10));
        queueTimeout.doRun();  // trigger the periodic work to run

        // Verify that the queue item was stopped
        verify(queueTimeout).submitStopQueueItemThread(any(Queue.Item.class), any(QueueTimeout.InvalidNodeInterruption.class));
        // TODO - find out how to verify that a static method is called and get the results
    }

    @Test
    public void testPipelineQueueTimeout() throws Exception {
        //FIXME - this test fails when run in class, but passes on its on. might be concurrency problem

        // Setting queue timeout of 1 minute
        GlobalTimeoutConfig.get().setQueueTimeout(1);

        // FIXME - test using a new agent
//        DumbSlave offline = jenkinsRule.createOnlineSlave(Label.parseExpression("offline"));
        DumbSlave offlineSlave = jenkinsRule.createSlave(Label.parseExpression("offline"));
        LOGGER.info("offlineSlave before disconnect: " + offlineSlave.getComputer().isOffline());
        offlineSlave.getComputer().doDoDisconnect("Setting offline");
        assertTrue("Slave should be offline", offlineSlave.getComputer().isOffline());


        // disconnecting agent
//        agent.getComputer().doDoDisconnect("Setting offline");
        Thread.sleep(TimeUnit.SECONDS.toMillis(5));
//        assertTrue(agent.getComputer().isOffline());

        WorkflowJob workflowJob = createWorkflowJob("offline");
        LOGGER.info("Scheduling a build");
        WorkflowRun workflowRun = workflowJob.scheduleBuild2(0).waitForStart();
        long queueId = workflowRun.getQueueId();
        Queue.Item queueItem = Queue.getInstance().getItem(queueId);
        assertTrue("agent should exist even when offline", QueueTimeout.checkIfAgentExists(queueItem));
        Thread.sleep(TimeUnit.MINUTES.toMillis(1) + TimeUnit.SECONDS.toMillis(20));
        assertEquals("There should be 1 queue item", 1, Queue.getInstance().getItems().length);
        LOGGER.info("workflowRun is building: " + workflowRun.isBuilding());
        queueTimeout.doRun();  // trigger the periodic work to run
        Thread.sleep(TimeUnit.SECONDS.toMillis(5));  // waiting 5 seconds
        verify(queueTimeout).submitStopQueueItemThread(any(Queue.Item.class), any(QueueTimeout.QueueTimeoutInterruption.class));
    }

    @Test
    public void testFreestylePass() throws Exception {

        GlobalTimeoutConfig.get().setQueueTimeout(1);
        FreeStyleProject freeStyleProject = jenkinsRule.createProject(FreeStyleProject.class, "freestyle-project");
        freeStyleProject.setAssignedLabel(new LabelAtom("valid-label"));
        FreeStyleBuild freeStyleBuild = freeStyleProject.scheduleBuild2(0).waitForStart();
        // verify that no queue task/item was cancelled
        verify(queue, never()).cancel(Mockito.any(Queue.Task.class));
        verify(queueTimeout, never()).submitStopQueueItemThread(any(Queue.Item.class), any(CauseOfInterruption.class));
    }

    /**
     * Create a pipeline job with the label
     * @param label Label to set in the workflow job (pipeline) definition
     * @return A workflow job project
     */
    private WorkflowJob createWorkflowJob(String label) throws IOException {
        WorkflowJob workflowJob = jenkinsRule.createProject(WorkflowJob.class, "test-pipeline");
        // TODO - test random name
        String pipelineString;
        if (Functions.isWindows()) {
            pipelineString = "node('" + label + "') { bat 'echo hi' }";
        } else {
            pipelineString = "node('" + label + "') { sh 'echo hi' }";
        }
        workflowJob.setDefinition(new CpsFlowDefinition(pipelineString, false));
        return workflowJob;
    }

    /**
     * Creates a pipeline job that lasts for a minimum of the duration
     * @param label Label to set in the workflow job definition
     * @param duration Minimum duration of the job should run
     * @return A workflow job project
     */
    private WorkflowJob createLongWorkflowJob(String label, int duration, String pipelineName) throws IOException {
        WorkflowJob workflowJob = jenkinsRule.createProject(WorkflowJob.class, pipelineName);
        String pipelineString;
        if (Functions.isWindows()) {
            pipelineString = "node('" + label + "') { bat 'timeout " + duration + "; echo hi' }";
        } else {
            pipelineString = "node('" + label + "') { sh 'sleep " + duration + "; echo hi' }";
        }
        workflowJob.setDefinition(new CpsFlowDefinition(pipelineString, false));
        return workflowJob;
    }
}
