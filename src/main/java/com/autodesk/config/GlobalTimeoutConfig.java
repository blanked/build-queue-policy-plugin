package com.autodesk.config;

import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.CheckForNull;

@Extension
public class GlobalTimeoutConfig extends GlobalConfiguration implements TimeoutConfig {

    private Integer buildTimeout;  // timeout in minutes. if timeout is 0, no timeout.
    private Integer queueTimeout;  // queue timeout in minutes. if timeout is 0, no timeout.

    public GlobalTimeoutConfig() {
        load();
    }

    public static GlobalTimeoutConfig get() {
        return GlobalConfiguration.all().get(GlobalTimeoutConfig.class);
    }

    /**
     * Gets the global timeout setting configured on the configure page
     * @return The timeout in minutes
     */
//    @Override
    @CheckForNull
    public Integer getBuildTimeout() {
        return buildTimeout;
    }

    @DataBoundSetter
    public void setBuildTimeout(Integer buildTimeout) {

        this.buildTimeout = buildTimeout;
        save();
    }


    @CheckForNull
    public Integer getQueueTimeout() {
        return queueTimeout;
    }

    @DataBoundSetter
    public void setQueueTimeout(Integer queueTimeout) {
        this.queueTimeout = queueTimeout;
        save();
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        req.bindJSON(this, json);
        return true;
    }
}
