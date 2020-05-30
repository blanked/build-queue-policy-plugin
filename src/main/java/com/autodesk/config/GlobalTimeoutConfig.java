package com.autodesk.config;

import hudson.Extension;
import jenkins.model.GlobalConfiguration;

import javax.annotation.CheckForNull;

@Extension
public class GlobalTimeoutConfig extends GlobalConfiguration implements TimeoutConfig {

    private Integer timeout;  // timeout in minutes. if timeout is 0, no timeout.

    public static GlobalTimeoutConfig get() {
        return GlobalConfiguration.all().get(GlobalTimeoutConfig.class);
    }

    private GlobalTimeoutConfig() {
        load();
    }

    /**
     * Gets the global timeout setting configured on the configure page
     * @return The timeout in minutes
     */
    @Override
    @CheckForNull
    public Integer getTimeout() {
        return timeout;
    }

    @Override
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }


}
