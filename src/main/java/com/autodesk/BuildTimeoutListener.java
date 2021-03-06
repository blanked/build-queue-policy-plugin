package com.autodesk;

import com.autodesk.config.GlobalTimeoutConfig;
import com.autodesk.config.JobTimeoutProperty;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Executor;
import hudson.model.JobProperty;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import jenkins.model.CauseOfInterruption;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import jenkins.util.Timer;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


@Extension
@Symbol("buildTimeoutListener")
public class BuildTimeoutListener extends RunListener<Run<?,?>> {

    private static final Logger LOGGER = Logger.getLogger(BuildTimeoutListener.class.getName());

    public BuildTimeoutListener() {

    }

    @Override
    public void onStarted(Run<?, ?> run, TaskListener listener) {

        // TODO - refactor to split the failure cause
        Integer timeout = getTimeout(run);
        if (timeout != null) {
            Timer.get().schedule(() -> {
                try {
                    Thread.currentThread().setName("Global Timeout plugin: " + run.getUrl());
                    abortBuild(run);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Abort Build failed for run " + run.getUrl(), e);
                } finally {
                    Thread.currentThread().setName("Global Timeout plugin: Idle thread in pool");
                }
            }, timeout, TimeUnit.MINUTES);
        }
    }

    private Integer getTimeout(Run run) {

        // check for job specific timeout
        JobProperty buildTimeoutProperty = run.getParent().getProperty(JobTimeoutProperty.class);
        if (buildTimeoutProperty instanceof JobTimeoutProperty) {
            Integer buildTimeout = ((JobTimeoutProperty) buildTimeoutProperty).getBuildTimeout();
            if (buildTimeout != null && buildTimeout > 0) {
                return buildTimeout;
            }
        }
        Integer globalTimeout = GlobalTimeoutConfig.get().getBuildTimeout();
        if (globalTimeout != null && globalTimeout > 0) {
            return globalTimeout;
        } else {
            return null;
        }
    }

    void abortBuild(Run run) throws IOException, ServletException, InterruptedException {
        if (run.isBuilding()) {
            // TODO - insert log to say that the build has been killed
            LOGGER.info("Timeout exceeded, interrupting run " + run.getUrl());
            Executor executor = run.getExecutor();
            if(executor == null) {
                LOGGER.warning("Executor is null for run " + run.getUrl());
                return;
            }
            executor.interrupt(Result.ABORTED, new JobTimeoutInterruption());

            // Wait for grace period duration to allow build to exit gracefully
            Integer gracePeriod = GlobalTimeoutConfig.get().getGracePeriod();
            Thread.sleep(TimeUnit.SECONDS.toMillis(gracePeriod));

            if (run.isBuilding()) {
                executor.doStop();
                if (run instanceof AbstractBuild) {
                    ((AbstractBuild) run).doStop();
                } else if (run instanceof WorkflowRun) {
                    ((WorkflowRun) run).doKill();
                }
            }
        }
    }

    public static class JobTimeoutInterruption extends CauseOfInterruption {

        @Override
        public String getShortDescription() {
            return "Job aborted due to timeout exceeded. Please contact the admins if you wish to find out more.";
        }
    }
}
