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
    public void unitTestGlobalTime() {
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
}
