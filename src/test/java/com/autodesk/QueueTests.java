package com.autodesk;

import com.autodesk.QueueTimeout;
import hudson.Functions;
import hudson.model.Computer;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.Slave;
import hudson.model.labels.LabelAtom;
import hudson.model.queue.SubTask;
import hudson.slaves.Cloud;
import hudson.slaves.DumbSlave;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;


/**
 * Test suite for all queue related features
 */
public class QueueTests {

    @Rule
    public final JenkinsRule jenkinsRule = new JenkinsRule();
    Logger LOGGER = Logger.getLogger(QueueTests.class.getName());
    Slave agent;


    @Before
    public void setup() throws Exception {
        agent = jenkinsRule.createOnlineSlave(new LabelAtom("test-label"));
    }

    @Test
    public void testStopQueueTask() {

        // Testing creation of a

    }

    @Test
    public void testCheckIfAgentExists() throws Exception {

        jenkinsRule.createOnlineSlave(new LabelAtom("test-label"));  // creating new agent
        // Testing invalid label
        FreeStyleProject freeStyleProject = jenkinsRule.createFreeStyleProject("invalid-node-freestyle");
        freeStyleProject.setAssignedLabel(new LabelAtom("non-existent-label"));
        LOGGER.info("Scheduling build");
        freeStyleProject.scheduleBuild2(0);
        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        boolean agentExists = QueueTimeout.checkIfAgentExists(freeStyleProject.getQueueItem());
        assertFalse(agentExists);
        // cancelling queue
        LOGGER.info("Cancelling previous scheduled freestyle build");
        Queue.getInstance().cancel(freeStyleProject.getQueueItem());
        // Testing valid label
        LOGGER.info("Starting new valid freestyle build");
        FreeStyleProject validFreeStyle = jenkinsRule.createFreeStyleProject("valid-freestyle");
        validFreeStyle.setAssignedLabel(new LabelAtom("test-label"));
        FreeStyleBuild validBuild = validFreeStyle.scheduleBuild2(0).get();
        long validBuildQueueId = validBuild.getQueueId();
        Queue.Item validQueueItem = Queue.getInstance().getItem(validBuildQueueId);
        LOGGER.info("validQueueItem id: " + validQueueItem.getId());
        LOGGER.info("validQueueItme name: " + validQueueItem.getDisplayName());
        Label assignedLabel = validQueueItem.getAssignedLabel();
        LOGGER.info("validQueueItem assigned label: " + assignedLabel.getExpression());
        Computer[] computers = jenkinsRule.getInstance().getComputers();
        for (Computer computer : computers) {
            Set<LabelAtom> assignedLabels = computer.getNode().getAssignedLabels();
            for (LabelAtom labelAtom : assignedLabels) {
                LOGGER.info("Label: " + labelAtom.getExpression());
            }
        }
        assertTrue(QueueTimeout.checkIfAgentExists(validQueueItem));  // FIXME - assertion yields false but should be true
    }

    // FIXME - this test is failing right now
    @Test
    public void testCheckIfAgentExistsPipeline() throws IOException, InterruptedException, ExecutionException {

        // Testing invalid label
        LOGGER.info("Creating pipeline job");
        WorkflowJob workflowJob = jenkinsRule.createProject(WorkflowJob.class, "test-pipeline");
        String pipelineString;
        if (Functions.isWindows()) {
            pipelineString = "node('non-existent-label') { bat 'echo hi' }";
        } else {
            pipelineString = "node('non-existent-label') { sh 'echo hi' }";
        }
        workflowJob.setDefinition(new CpsFlowDefinition(pipelineString, false));
        LOGGER.info("Scheduling build");
        Future<WorkflowRun> startCondition = workflowJob.scheduleBuild2(0).getStartCondition();
        assertNotNull(startCondition);
        WorkflowRun workflowRun = startCondition.get();
        assertNotNull(workflowRun);
//        WorkflowRun workflowRun = workflowJob.scheduleBuild2(0).getStartCondition().get();
        LOGGER.info("Build has been scheduled");
        long queueId = workflowRun.getQueueId();
        LOGGER.info("Queue id: " + queueId);
        Queue.Item item = Queue.getInstance().getItem(queueId);
        LOGGER.info("Queue: " + item.getDisplayName());
        LOGGER.info("Queue expression: " + item.getAssignedLabel().getExpression());
//        assertFalse(QueueTimeout.checkIfAgentExists(item));
        // FIXME - The master task was obtained instead

        Collection<? extends SubTask> subTasks = item.task.getSubTasks();
        for (SubTask subTask : subTasks) {
            LOGGER.info("SubTask: " + subTask.getDisplayName());
            LOGGER.info("SubTask agent: " + subTask.getAssignedLabel());
        }
        Queue.Item[] items = Queue.getInstance().getItems();
        for (Queue.Item queueItem : items) {
            LOGGER.info("Queue item: " + queueItem.getDisplayName() + "  label: " + queueItem.getAssignedLabel());
        }

        Thread.sleep(TimeUnit.SECONDS.toMillis(1));  // wait 1 sec
        boolean agentExists = QueueTimeout.checkIfAgentExists(item);
        assertFalse(agentExists);
        // Testing valid label

    }

    @Test
    public void testValidPipelineLabel() throws Exception {
        WorkflowJob workflowJob = jenkinsRule.createProject(WorkflowJob.class, "working-pipeline");
        String pipelineString;
        if (Functions.isWindows()) {
            pipelineString = "node('test-label') { bat 'echo hi' }";
        } else {
            pipelineString = "node('test-label') { sh 'echo hi' }";
        }
        workflowJob.setDefinition(new CpsFlowDefinition(pipelineString, false));
        WorkflowRun workflowRun = workflowJob.scheduleBuild2(0).get();
        long queueId = workflowRun.getQueueId();
        Queue.Item item = Queue.getInstance().getItem(queueId);
        assertNotNull(item);
        assertTrue(QueueTimeout.checkIfAgentExists(item));
        jenkinsRule.assertBuildStatus(Result.SUCCESS, workflowRun);
    }
}
