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
import hudson.model.AdministrativeMonitor;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.interceptor.RequirePOST;

/**
 * Tells administrators that a restart is pending while {@link RestartDrain} is active. Core shows
 * its own banner only once quiet-down starts, which happens after the last build finishes.
 */
@Extension
public class RestartDrainAdministrativeMonitor extends AdministrativeMonitor {

    @Override
    public boolean isActivated() {
        return RestartDrain.get().isActive();
    }

    @Override
    public String getDisplayName() {
        return Messages.RestartDrainAdministrativeMonitor_displayName();
    }

    /**
     * @return number of builds that still hold the restart
     */
    public int getRunningCount() {
        return RestartDrain.get().getRunningBuilds().size();
    }

    /**
     * Cancels the drain from the monitor and returns to the Manage Jenkins page.
     */
    @RequirePOST
    public HttpResponse doCancel() {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        RestartDrain.get().cancel();
        return HttpResponses.redirectViaContextPath("manage");
    }
}
