package com.autodesk;

import com.autodesk.config.GlobalTimeoutConfig;
import hudson.Functions;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.labels.LabelAtom;
import hudson.slaves.DumbSlave;
import jenkins.model.CauseOfInterruption;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import org.mockito.Mockito;
import org.mockito.Spy;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

/**
 * Test suite for Queue Timeout using mock
 */
public class MockQueueTests {

    // TODO - mock a queue
    @Spy
    @Rule
    public final JenkinsRule jenkinsRule = new JenkinsRule();
    @Spy
    Queue queue;
    private final Logger LOGGER = Logger.getLogger(MockQueueTests.class.getName());
    QueueTimeout queueTimeout;
    DumbSlave agent;

    @Before
    public void setup() throws Exception {
        // Getting a spy of the queue
        queue = spy(Queue.getInstance());
        agent = jenkinsRule.createOnlineSlave(new LabelAtom("valid-label"));  // creating a valid test label
        queueTimeout = spy(QueueTimeout.all().get(QueueTimeout.class));
        // Setting the queue timeout
        GlobalTimeoutConfig.get().setQueueTimeout(1);
        GlobalTimeoutConfig.get().setNoSuchNodeQueueTimeout(1);
    }

    @Test
    public void testPipelinePass() throws Exception {
//        @Spy
        /*
        TODO - mock a queue and submit an invalid job. If the job is invalid, the task should be resubmitted
         */
        WorkflowJob workflowJob = createWorkflowJob("valid-label");
        WorkflowRun workflowRun = workflowJob.scheduleBuild2(0).get();

        // verify that no queue task/item was cancelled
        verify(queue, never()).cancel(Mockito.any(Queue.Task.class));
        verify(queueTimeout, never()).submitStopQueueItemThread(any(Queue.Item.class), any(CauseOfInterruption.class));

        // verify that the build succeeded
        jenkinsRule.assertBuildStatus(Result.SUCCESS, workflowRun);
    }

    @Test
    public void testPipelineInvalidNode() throws Exception {

        WorkflowJob workflowJob = createWorkflowJob("invalid-label");
        WorkflowRun workflowRun = workflowJob.scheduleBuild2(0).get();

        // Verify that the queue item was stopped
        verify(queueTimeout).submitStopQueueItemThread(any(Queue.Item.class), any(QueueTimeout.InvalidNodeInterruption.class));
        // Verify that the workflowrun was not executed successfully
        jenkinsRule.assertBuildStatus(Result.ABORTED, workflowRun);
    }

    @Test
    public void testPipelineQueueTimeout() throws Exception {
        // disconnecting agent
        agent.getComputer().doDoDisconnect("Setting offline");
        assertTrue(agent.getComputer().isOffline());
    }

    @Test
    public void testFreestylePass() throws Exception {

    }

    /**
     * Create a pipeline job with the label
     * @param label Label to set in the workflow job (pipeline) definition
     * @return A workflow job project
     */
    private WorkflowJob createWorkflowJob(String label) throws IOException {
        WorkflowJob workflowJob = jenkinsRule.createProject(WorkflowJob.class, "working-pipeline");
        String pipelineString;
        if (Functions.isWindows()) {
            pipelineString = "node('" + label + "') { bat 'echo hi' }";
        } else {
            pipelineString = "node('" + label + "') { sh 'echo hi' }";
        }
        workflowJob.setDefinition(new CpsFlowDefinition(pipelineString, false));
        return workflowJob;
    }
}
