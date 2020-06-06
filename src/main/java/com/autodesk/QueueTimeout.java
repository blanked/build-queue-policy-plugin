package com.autodesk;

import com.autodesk.config.GlobalTimeoutConfig;
import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Label;
import hudson.model.PeriodicWork;
import hudson.model.Queue;
import hudson.model.labels.LabelAtom;
import hudson.model.queue.QueueListener;
import hudson.slaves.Cloud;
import jenkins.model.Jenkins;
import jenkins.util.Timer;
import org.jenkinsci.Symbol;
import static hudson.model.Computer.threadPoolForRemoting;
import org.jenkinsci.plugins.workflow.support.steps.ExecutorStepExecution;
import org.kohsuke.stapler.DataBoundSetter;

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

    public void checkForExpired() {
        Queue.Item[] queueItems = queue.getItems();
        Integer queueTimeout = GlobalTimeoutConfig.get().getQueueTimeout();
        Integer noSuchNodeQueueTimeout = GlobalTimeoutConfig.get().getNoSuchNodeQueueTimeout();
        for (Queue.Item queueItem : queueItems) {
            // check if queue has exceeded
            if (queueTimeout != null) {
                long timeElapsedMillis = System.currentTimeMillis() - queueItem.getInQueueSince();
                if (TimeUnit.MILLISECONDS.toMinutes(timeElapsedMillis) > queueTimeout) {
                    scheduleCancel(queueItem);
                }
            }
        }
    }

    /**
     * Method is executed periodically based on {@link QueueTimeout#getRecurrencePeriod()} return value to check if the
     * queue has any expired items
     * @throws Exception
     */
    @Override
    protected void doRun() throws Exception {

        checkForExpired();
    }

    private void scheduleCancel(Queue.Item queueItem) {

        threadPoolForRemoting.submit(() -> {
            Thread.currentThread().setName("Cancel queue thread: " + queueItem.getDisplayName());
            queue.cancel(queueItem);
            // TODO - find out a way to notify on the build on why its cancelled
        });
    }

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

    /**
     * Checks if there will be an available agent for the
     * @param queueItem
     * @return
     */
    private static boolean checkIfAgentExists(Queue.Item queueItem) {

        Label assignedLabel = queueItem.getAssignedLabel();
        if (assignedLabel == null) {
            return true;
        }
        Computer[] computers = jenkins.getComputers();
        Set<Cloud> clouds = assignedLabel.getClouds();
        if (clouds.size() > 0) {
            return true;
        }
        for (Computer computer : computers) {
            Set<LabelAtom> assignedLabels = computer.getAssignedLabels();
            for (LabelAtom labelAtom : assignedLabels) {
                if (labelAtom.getExpression().equals(assignedLabel.getExpression())) {
                    return true;
                }
            }
        }
        return false;
    }

    /*
    TODO - consider below timeouts
    final timeout - hard limit for all queue
    executor starvation - no online agent
    no such agent - no agent and no cloud has this label
     */
}
