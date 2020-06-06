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
    private Integer noSuchNodeQueueTimeout;

    public GlobalTimeoutConfig() {
        load();
    }

    public static GlobalTimeoutConfig get() {
        return GlobalConfiguration.all().get(GlobalTimeoutConfig.class);
    }

    /**
     * Sets the build timeout setting. Configured on the configure page of Jenkins
     * @param buildTimeout The build timeout in minutes
     */
    @DataBoundSetter
    public void setBuildTimeout(Integer buildTimeout) {

        this.buildTimeout = buildTimeout;
        save();
    }

    /**
     * Gets the global timeout setting configured on the configure page of Jenkins
     * @return The build timeout in minutes
     */
    @CheckForNull
    public Integer getBuildTimeout() {
        return buildTimeout;
    }


    /**
     * Sets the queue timeout setting. Configured on the configure page of Jenkins
     * @param queueTimeout The queue timeout in minutes
     */
    @DataBoundSetter
    public void setQueueTimeout(Integer queueTimeout) {
        this.queueTimeout = queueTimeout;
        save();
    }

    /**
     * Gets the queue timeout setting configured on the configurte page of Jenkins
     * @return The queue timeout in minutes
     */
    @CheckForNull
    public Integer getQueueTimeout() {
        return queueTimeout;
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        req.bindJSON(this, json);
        return true;
    }

    /**
     * Sets the "No such node" queue timeout setting
     * @param noSuchNodeQueueTimeout The timeout setting in minutes
     */
    @DataBoundSetter
    public void setNoSuchNodeQueueTimeout(Integer noSuchNodeQueueTimeout) {
        this.noSuchNodeQueueTimeout = noSuchNodeQueueTimeout;
    }

    /**
     * Gets the "No such node" queue timeout setting. This setting times is the time limit allowed for builds that are
     * created with an invalid label (no nodes and clouds have such a label)
     * @return The timeout setting in minutes
     */
    public Integer getNoSuchNodeQueueTimeout() {
        return noSuchNodeQueueTimeout;
    }
}
