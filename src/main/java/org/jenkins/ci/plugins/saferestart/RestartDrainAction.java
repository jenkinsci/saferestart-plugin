/*
 * The MIT License
 *
 * Copyright (c) 2010-2011, Seiji Sogabe, Jesse Farinacci
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.jenkins.ci.plugins.saferestart;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.Extension;
import hudson.Util;
import hudson.model.RootAction;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;

/**
 * Page and side-panel link for {@link RestartDrain}: start or cancel the drain and see which
 * builds still hold the restart.
 */
@Extension
public class RestartDrainAction implements RootAction {

    @Override
    public String getDisplayName() {
        return Messages.RestartDrainAction_displayName();
    }

    /**
     * Hidden when the "wait for running builds" option is on: the regular "Restart Safely" links
     * lead here already, so a second link would be a duplicate. The page stays reachable.
     */
    @Override
    @CheckForNull
    public String getIconFileName() {
        if (!JenkinsUtils.hasAdministerPermission() || !JenkinsUtils.canRestart()) {
            return null;
        }
        if (SafeRestartConfiguration.get().isWaitForRunningBuilds()) {
            return null;
        }
        return Constants.ICON;
    }

    @Override
    public String getUrlName() {
        return Constants.DRAIN_URL;
    }

    public RestartDrain getDrain() {
        return RestartDrain.get();
    }

    /**
     * @return how long ago the drain started, for display
     */
    public String getSinceAgo() {
        return Util.getTimeSpanString(
                System.currentTimeMillis() - RestartDrain.get().getSince());
    }

    /**
     * Starts the drain. Restarts right away if nothing is running.
     *
     * @param message optional message for the shutdown banner
     */
    @RequirePOST
    public HttpResponse doStart(@QueryParameter String message) {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        RestartDrain.get().start(message);
        return HttpResponses.redirectToDot();
    }

    /**
     * Cancels the drain. Held-back builds start again.
     */
    @RequirePOST
    public HttpResponse doCancel() {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        RestartDrain.get().cancel();
        return HttpResponses.redirectToDot();
    }
}
