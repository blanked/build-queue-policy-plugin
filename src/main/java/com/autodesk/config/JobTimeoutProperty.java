package com.autodesk.config;

import hudson.Extension;
import hudson.model.Job;
import hudson.model.JobProperty;
import jenkins.model.OptionalJobProperty;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;

/**
 * Optional job property to override the Global Timeout setting configured in the configure page
 */
@Restricted(NoExternalUse.class)
public class JobTimeoutProperty extends OptionalJobProperty<Job<?,?>> implements TimeoutConfig {
    Integer buildTimeout;

    @DataBoundConstructor
    public JobTimeoutProperty(Integer buildTimeout) {
        if (buildTimeout < 0) {
            this.buildTimeout = 0;
        } else {
            this.buildTimeout = buildTimeout;
        }
    }

    @Override
    public Integer getBuildTimeout() {
        return this.buildTimeout;
    }

    @Override
    public void setBuildTimeout(Integer buildTimeout) {
        this.buildTimeout = buildTimeout;
    }

    @Extension
    @Symbol("jobTimeoutProperty")
    public static class DescriptorImpl extends OptionalJobPropertyDescriptor {

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Set a Job timeout property (overrides any globally configured timeout)";
        }

        @Override
        public JobProperty<?> newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return super.newInstance(req, formData);
        }
    }
}
