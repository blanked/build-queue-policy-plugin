package com.autodesk.config;

import hudson.model.FreeStyleProject;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
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
 * Test suite for {@link JobTimeoutProperty}
 */
public class JobTimeoutPropertyTests {

    @Rule
    public final JenkinsRule jenkinsRule = new JenkinsRule();
    Logger LOGGER = Logger.getLogger(JobTimeoutPropertyTests.class.getName());
    FreeStyleProject freeStyleProject;
    WorkflowJob pipelineProject;

    @Before
    public void setup() throws Exception {

        LOGGER.info("Running setup method");
        for (int i=0; i<2; i++) {
            jenkinsRule.createOnlineSlave();
        }
        // Setting up freestyle project
        freeStyleProject = jenkinsRule.createProject(FreeStyleProject.class, "freestyle");
        // Setting up pipeline project
        pipelineProject = jenkinsRule.createProject(WorkflowJob.class, "pipeline");
        LOGGER.info("Setup method complete!");
    }

    @Test
    public void testSetJobTimeoutProperty() throws IOException {
        freeStyleProject.addProperty(new JobTimeoutProperty(1));
        JobTimeoutProperty property = freeStyleProject.getProperty(JobTimeoutProperty.class);
        Integer buildTimeout = property.getBuildTimeout();
        assertEquals(1, buildTimeout.intValue());
    }

    @Test
    public void testPipelineSetJobProperty() throws ExecutionException, InterruptedException {
        String jenkinsfileString = "node() {\n" +
                "    properties([jobTimeoutProperty(1)])\n" +
                "    echo 'hi'" +
                "}";
        pipelineProject.setDefinition(new CpsFlowDefinition(jenkinsfileString, false));
        Objects.requireNonNull(pipelineProject.scheduleBuild2(0)).get();
        JobTimeoutProperty property = pipelineProject.getProperty(JobTimeoutProperty.class);
        assertEquals(1, property.getBuildTimeout().intValue());
    }
}
