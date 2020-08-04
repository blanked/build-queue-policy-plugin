package com.autodesk;

import com.autodesk.config.GlobalTimeoutConfig;
import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Executor;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import hudson.model.PeriodicWork;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.labels.LabelAtom;
import hudson.model.queue.QueueListener;
import hudson.slaves.Cloud;
import jenkins.model.CauseOfInterruption;
import jenkins.model.Jenkins;
import jenkins.util.Timer;
import org.jenkinsci.Symbol;
import static hudson.model.Computer.threadPoolForRemoting;

import org.jenkinsci.plugins.workflow.support.steps.ExecutorStepExecution;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
@Symbol("queueTimeout")
public class QueueTimeout extends PeriodicWork {

    private final Logger LOGGER = Logger.getLogger(this.getClass().getName());
    private final static Jenkins jenkins = Jenkins.get();
    private final Queue queue = jenkins.getQueue();

    @Override
    public long getRecurrencePeriod() {
        return TimeUnit.MINUTES.toMillis(1);  // run every 1 minute
    }

    /**
     * Method is executed periodically based on {@link QueueTimeout#getRecurrencePeriod()} return value to check if the
     * queue has any expired items
     * @throws Exception
     */
    @Override
    protected void doRun() throws Exception {

        checkForTimeout();
    }

    /**
     * Performs a check on whether there are any queue items that have timed out
     */
    public void checkForTimeout() {
        Queue.Item[] queueItems = queue.getItems();
        Integer queueTimeout = GlobalTimeoutConfig.get().getQueueTimeout();
        Integer noSuchNodeQueueTimeout = GlobalTimeoutConfig.get().getNoSuchNodeQueueTimeout();
        for (Queue.Item queueItem : queueItems) {
            // check if queue timeout has exceeded
            if (queueTimeout != null) {
                long timeElapsedMillis = System.currentTimeMillis() - queueItem.getInQueueSince();
                if (TimeUnit.MILLISECONDS.toMinutes(timeElapsedMillis) > queueTimeout) {
                    submitStopQueueItemThread(queueItem, new QueueTimeoutInterruption());
                }
            }
            // TODO - check if there are queueItems without a valid label
            boolean agentExists = checkIfAgentExists(queueItem);
            if (!agentExists && noSuchNodeQueueTimeout != null && noSuchNodeQueueTimeout > 0) {
                // Checking elapsed time
                long timeElapsedMillis = System.currentTimeMillis() - queueItem.getInQueueSince();
                if (TimeUnit.MILLISECONDS.toMinutes(timeElapsedMillis) > noSuchNodeQueueTimeout) {
                    Label assignedLabel = queueItem.getAssignedLabel();
                    String labelExpression = assignedLabel.getExpression();
                    submitStopQueueItemThread(queueItem, new InvalidNodeInterruption(labelExpression));
                }
            }
        }
    }

    /**
     * Submits a thread to stop the queued item
     * @param queueItem The queue item to stop
     */
    void submitStopQueueItemThread(Queue.Item queueItem, CauseOfInterruption cause) {

        threadPoolForRemoting.submit(() -> {
            Thread.currentThread().setName("Cancel queue thread: " + queueItem.getDisplayName());
//            queue.cancel(queueItem);  // TODO - don't simply do queue.cancel. Instead interrupt the task
            Queue.Task task = queueItem.task;
            stopQueueTask(task, cause);
        });
    }

    /**
     * Stops or interrupts the queue task
     * @param task Queue task to be stopped
     * @param cause Cause of stoppage to be flagged in aborted run ({@link QueueTimeoutInterruption} or {@link InvalidNodeInterruption}
     */
    private void stopQueueTask(Queue.Task task, CauseOfInterruption cause) {

        if (task instanceof ExecutorStepExecution.PlaceholderTask) {  // pipeline task
            // Queue.Task ownerTask = task.getOwnerTask();
            Run<?, ?> run = ((ExecutorStepExecution.PlaceholderTask) task).run();
            if (run == null) {
                LOGGER.warning("Run of task is null: " + task.getUrl());
                return;
            }
            Executor executor = run.getExecutor();
            if (executor == null) {
                LOGGER.warning("Executor for run is null: " + run.getUrl());
                return;
            }
            executor.interrupt(Result.ABORTED, cause);
        } else if (task instanceof FreeStyleProject) {  // freestyle task
            // TODO - implement a way to notify freestyleproject some day
            queue.cancel(task);
        }
    }

    /**
     * Cause of interruption for a Queue timeout event (when a Queue Item remains in queue for too long)
     */
    public static class QueueTimeoutInterruption extends CauseOfInterruption {

        @Override
        public String getShortDescription() {
            Integer queueTimeout = GlobalTimeoutConfig.get().getQueueTimeout();
            return String.format("Queue task cancelled due to exceeding queue time limit of %s minutes", queueTimeout);
        }
    }

    /**
     * Cause of interruption for an Invalid Node event (when a Queue item has a label with no associated agents)
     */
    public static class InvalidNodeInterruption extends CauseOfInterruption {

        String label;
        InvalidNodeInterruption(String label) {
            this.label = label;
        }

        @Override
        public String getShortDescription() {
            Integer noSuchNodeQueueTimeout = GlobalTimeoutConfig.get().getNoSuchNodeQueueTimeout();
            return String.format("Queue task cancelled after %s as there are no agents associated with the label " +
                    "used: %s", noSuchNodeQueueTimeout, label);
        }
    }

    /**
     * Checks if there will be a valid agent (might be offline) for a queue item
     * @param queueItem The queue item to check against
     * @return true if there is a valid agent (both online or offline) associated with this label
     */
    static boolean checkIfAgentExists(Queue.Item queueItem) {

        Label assignedLabel = queueItem.getAssignedLabel();
        if (assignedLabel == null) {  // no label set. All agents are valid agents
            return true;
        }
        // Checking for dynamic cloud agents
        Set<Cloud> clouds = assignedLabel.getClouds();
        if (clouds.size() > 0) {
            return true;
        }
        // Checking for static agents
        Computer[] computers = jenkins.getComputers();
        for (Computer computer : computers) {
            Set<LabelAtom> assignedLabels = computer.getNode().getAssignedLabels();
            for (LabelAtom labelAtom : assignedLabels) {
                if (labelAtom.getExpression().equals(assignedLabel.getExpression())) {
                    return true;
                }
            }
        }
        return false;
    }

    // TODO - verify if this is still necessary. may create a lot of threads. might be better to just use the periodicwork
    /**
     * A {@link QueueListener} that listens for an Invalid Node event (Queue item with a label without associated agents)
     */
    @Extension
    public static class InvalidLabelListener extends QueueListener {

        private static Logger LOGGER = Logger.getLogger(QueueTimeout.class.getName());
        /**
         * Method is automatically executed on every new queue item. If a queue item has a label that is invalid, it
         * will be cancelled once it exceeds the "noSuchNodeQueueTimeout" setting (if set).
         * @param wi Queue Waiting Item
         */
        @Override
        public void onEnterWaiting(Queue.WaitingItem wi) {

            Integer noSuchNodeQueueTimeout = GlobalTimeoutConfig.get().getNoSuchNodeQueueTimeout();
            if (noSuchNodeQueueTimeout != null) {
                if (!QueueTimeout.checkIfAgentExists(wi)) {
                    Timer.get().schedule(() -> {
                        String originalThreadName = Thread.currentThread().getName();
                        Thread.currentThread().setName("global-build-timeout plugin: invalid queue task - "
                                + wi.task.getUrl());
                        try {
                            Queue queue = Jenkins.get().getQueue();
                            queue.cancel(wi);
                        } catch (Exception e) {
                            LOGGER.log(Level.INFO, "Exception occurred while cancelling queue task "
                                    + wi.task.getUrl(), e);
                        } finally {
                            Thread.currentThread().setName(originalThreadName);
                        }
                    }, noSuchNodeQueueTimeout, TimeUnit.MINUTES);
                }
            }
        }
    }
}
