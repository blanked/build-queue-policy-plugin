package com.autodesk;

import hudson.Extension;
import hudson.model.Queue;
import hudson.model.queue.QueueListener;

import java.util.logging.Logger;

@Extension
public class QueueTimeoutListener extends QueueListener {

    private final Logger LOGGER = Logger.getLogger(this.getClass().getName());

    @Override
    public void onEnterWaiting(Queue.WaitingItem wi) {
        super.onEnterWaiting(wi);
        // TODO - schedule a thread to check on the status?
        // alternative is to run it every minute and check for the queue that is overdue]
    }
}
