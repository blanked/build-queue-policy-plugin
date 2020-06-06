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
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;

/**
 * Optional job property to override the Global Timeout setting configured in the configure page
 */
@Restricted(NoExternalUse.class)
public class JobTimeoutProperty extends OptionalJobProperty<Job<?,?>> implements TimeoutConfig {
    Integer timeout;

    @DataBoundConstructor
    public JobTimeoutProperty(Integer timeout) {
        if (timeout < 0) {
            this.timeout = 0;
        } else {
            this.timeout = timeout;
        }
    }

    @Override
    public OptionalJobPropertyDescriptor getDescriptor() {
        return super.getDescriptor();
    }

    @Override
    public Integer getBuildTimeout() {
        return this.timeout;
    }

    @Override
    public void setBuildTimeout(Integer buildTimeout) {
        this.timeout = buildTimeout;
    }

    // TODO - implement the jelly bits to enable adding of config for freestyle
    @Extension
    @Symbol("jobTimeoutProperty")
    public static class DescriptorImpl extends OptionalJobPropertyDescriptor {

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Job timeout property that overrides the globally configured timeout";
        }

        @Override
        public JobProperty<?> newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return super.newInstance(req, formData);
        }
    }

}
