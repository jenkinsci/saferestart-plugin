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

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.ExtensionList;
import jenkins.model.GlobalConfiguration;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Global settings of the plugin, shown under Manage Jenkins, System.
 */
@Extension
@Symbol("safeRestart")
public final class SafeRestartConfiguration extends GlobalConfiguration {

    private boolean waitForRunningBuilds;

    public SafeRestartConfiguration() {
        load();
    }

    public static SafeRestartConfiguration get() {
        return ExtensionList.lookupSingleton(SafeRestartConfiguration.class);
    }

    /**
     * @return true if the plugin's "Restart Safely" links use {@link RestartDrain} instead of the
     *     core safe restart
     */
    public boolean isWaitForRunningBuilds() {
        return waitForRunningBuilds;
    }

    @DataBoundSetter
    public void setWaitForRunningBuilds(boolean waitForRunningBuilds) {
        this.waitForRunningBuilds = waitForRunningBuilds;
        save();
    }

    @NonNull
    @Override
    public String getDisplayName() {
        return Messages.SafeRestartConfiguration_displayName();
    }
}
