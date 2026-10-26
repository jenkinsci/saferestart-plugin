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

import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.RootAction;
import org.kohsuke.stapler.StaplerProxy;

/**
 * Action for SafeRestart. Added restart link to left-side panel.
 *
 * <p>With "wait for running builds" enabled in {@link SafeRestartConfiguration}, the link leads to
 * the {@link RestartDrainAction} page instead of the core safe restart.
 *
 * @author Seiji Sogabe
 * @author <a href="mailto:jieryn@gmail.com">Jesse Farinacci</a>
 */
@Extension
public class SafeRestartRootAction implements RootAction, StaplerProxy {

    @Override
    public String getDisplayName() {
        return Messages.SafeRestartRootAction_displayName();
    }

    @Override
    public String getIconFileName() {
        if (!JenkinsUtils.hasAdministerPermission()) {
            return null;
        }

        if (!JenkinsUtils.canRestart()) {
            return null;
        }

        return Constants.ICON;
    }

    @Override
    public String getUrlName() {
        if (SafeRestartConfiguration.get().isWaitForRunningBuilds()) {
            return Constants.DRAIN_URL;
        }
        return Constants.RESTART_URL;
    }

    /**
     * {@link jenkins.model.Jenkins#getDynamic} may resolve the drain URL to this action because the
     * URL names match, so requests are forwarded to {@link RestartDrainAction} in that mode.
     */
    @Override
    public Object getTarget() {
        if (SafeRestartConfiguration.get().isWaitForRunningBuilds()) {
            return ExtensionList.lookupSingleton(RestartDrainAction.class);
        }
        return this;
    }
}
