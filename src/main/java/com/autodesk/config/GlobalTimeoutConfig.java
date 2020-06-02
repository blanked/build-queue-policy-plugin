package com.autodesk.config;

import hudson.Extension;
import jenkins.model.GlobalConfiguration;

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

    @Override
    public void setBuildTimeout(int buildTimeout) {
        this.buildTimeout = buildTimeout;
    }


    public Integer getQueueTimeout() {
        return queueTimeout;
    }

    public void setQueueTimeout(Integer queueTimeout) {
        this.queueTimeout = queueTimeout;
    }
}
