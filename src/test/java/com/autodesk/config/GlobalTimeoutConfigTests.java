package com.autodesk.config;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import static org.junit.Assert.*;

public class GlobalTimeoutConfigTests {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Test
    public void testSetBuildTimeout() {
        GlobalTimeoutConfig.get().setBuildTimeout(1);
        Integer buildTimeout = GlobalTimeoutConfig.get().getBuildTimeout();
        assertNotNull(buildTimeout);
        assertEquals(1, buildTimeout.intValue());
    }

    @Test
    public void testSetQueueTimeout() {
        GlobalTimeoutConfig.get().setQueueTimeout(1);
        Integer queueTimeout = GlobalTimeoutConfig.get().getQueueTimeout();
        assertNotNull(queueTimeout);
        assertEquals(1, queueTimeout.intValue());
    }

    @Test
    public void testSetNoSuchNodeQueueTimeout() {
        GlobalTimeoutConfig.get().setNoSuchNodeQueueTimeout(1);
        Integer noSuchNodeQueueTimeout = GlobalTimeoutConfig.get().getNoSuchNodeQueueTimeout();
        assertNotNull(noSuchNodeQueueTimeout);
        assertEquals(1, noSuchNodeQueueTimeout.intValue());
    }

    @Test
    public void testSetGracePeriod() {
        GlobalTimeoutConfig.get().setGracePeriod(1);
        Integer gracePeriod = GlobalTimeoutConfig.get().getGracePeriod();
        assertNotNull(gracePeriod);
        assertEquals(1, gracePeriod.intValue());
    }
    
}
