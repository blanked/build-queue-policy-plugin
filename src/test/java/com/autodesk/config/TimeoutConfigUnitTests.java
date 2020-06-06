package com.autodesk.config;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.*;

/**
 * Test class for {@link GlobalTimeoutConfig}
 */
public class TimeoutConfigUnitTests {

    @Rule
    public final JenkinsRule jenkinsRule = new JenkinsRule();

    @Test
    public void unitTestBuildTimeout() {
        Integer testTimeout = 10;
        GlobalTimeoutConfig globalTimeoutConfig = GlobalTimeoutConfig.get();
        globalTimeoutConfig.setBuildTimeout(testTimeout);
        assertEquals(testTimeout, globalTimeoutConfig.getBuildTimeout());
    }

    @Test
    public void unitTestJobTimeout() {
        Integer testTimeout = 10;
        JobTimeoutProperty property = new JobTimeoutProperty(testTimeout);
        assertEquals(testTimeout, property.getBuildTimeout());
    }

    @Test
    public void unitTestQueueTimeout() {
        Integer testTimeout = 10;
        GlobalTimeoutConfig config = GlobalTimeoutConfig.get();
        config.setQueueTimeout(testTimeout);
        assertEquals(testTimeout, config.getQueueTimeout());
    }

    @Test
    public void unitTestNoSuchNodeQueueTimeout() {
        Integer testTimeout = 10;
        GlobalTimeoutConfig config = GlobalTimeoutConfig.get();
        config.setNoSuchNodeQueueTimeout(testTimeout);
        assertEquals(testTimeout, config.getNoSuchNodeQueueTimeout());
    }
}
