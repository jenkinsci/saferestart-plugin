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
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.Util;
import hudson.model.Items;
import hudson.model.Job;
import hudson.model.Run;
import hudson.security.ACL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import jenkins.model.lazy.LazyBuildMixIn;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Drain mode: block new top-level builds, let running builds finish, then trigger a safe restart.
 *
 * <p>Core quiet-down cannot be used for this. It also blocks the Pipeline {@code node} steps of
 * builds that are already running, so those builds could never finish and the restart would never
 * happen. Drain mode only holds back new builds, see {@link RestartDrainQueueTaskDispatcher}, and
 * enters core quiet-down through {@link Jenkins#safeRestart(String)} once nothing is running.
 *
 * <p>The state is transient. A restart or crash while draining resets it.
 */
@Extension
public class RestartDrain implements ExtensionPoint {

    private static final Logger LOGGER = Logger.getLogger(RestartDrain.class.getName());

    /**
     * Performs the restart once nothing is running. Replaceable so tests do not restart the JVM.
     */
    @FunctionalInterface
    interface Restarter {
        void restart(@CheckForNull String message) throws Exception;
    }

    private volatile boolean active;

    @CheckForNull
    private volatile String message;

    @CheckForNull
    private volatile String startedBy;

    private volatile long since;

    private volatile Restarter restarter = m -> Jenkins.get().safeRestart(m);

    public static RestartDrain get() {
        return ExtensionList.lookupSingleton(RestartDrain.class);
    }

    /**
     * @return true while new builds are held back and the restart is pending
     */
    public boolean isActive() {
        return active;
    }

    /**
     * @return the message shown in the shutdown banner once the restart starts, or null
     */
    @CheckForNull
    public String getMessage() {
        return message;
    }

    /**
     * @return name of the user who started the drain, or null when not active
     */
    @CheckForNull
    public String getStartedBy() {
        return startedBy;
    }

    /**
     * @return epoch milliseconds when the drain started, or 0 when not active
     */
    public long getSince() {
        return since;
    }

    /**
     * Starts holding back new builds. Restarts right away if nothing is running.
     * Does nothing if already active.
     *
     * @param message optional message for the shutdown banner
     */
    public void start(@CheckForNull String message) {
        synchronized (this) {
            if (active) {
                return;
            }
            active = true;
            this.message = Util.fixEmptyAndTrim(message);
            startedBy = Jenkins.getAuthentication2().getName();
            since = System.currentTimeMillis();
        }
        LOGGER.log(Level.INFO, "Safe restart drain started by {0}", startedBy);
        Jenkins.get().getQueue().scheduleMaintenance();
        checkAndRestart();
    }

    /**
     * Stops holding back new builds. No restart happens.
     */
    public void cancel() {
        synchronized (this) {
            if (!active) {
                return;
            }
            reset();
        }
        LOGGER.info("Safe restart drain cancelled");
        Jenkins.get().getQueue().scheduleMaintenance();
    }

    /**
     * Triggers the restart if the drain is active and nothing is running. Safe to call from any
     * thread; the drain is deactivated before the restart is triggered so it fires only once.
     */
    public void checkAndRestart() {
        if (!active) {
            return;
        }
        List<Run<?, ?>> running = getRunningBuilds();
        if (!running.isEmpty()) {
            LOGGER.log(Level.FINE, "Safe restart is waiting for {0} running build(s): {1}", new Object[] {
                running.size(), running
            });
            return;
        }
        String banner;
        synchronized (this) {
            if (!active) {
                return;
            }
            banner = message;
            reset();
        }
        Jenkins.get().getQueue().scheduleMaintenance();
        LOGGER.info("No builds are running, triggering the safe restart");
        try {
            restarter.restart(banner);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to trigger the safe restart", e);
        }
    }

    /**
     * Builds that are still running. For lazy-loading job types only builds already in memory are
     * inspected: a running build is always loaded, and this avoids reading build records from disk
     * on every check.
     *
     * @return running builds, never null
     */
    @NonNull
    public List<Run<?, ?>> getRunningBuilds() {
        List<Run<?, ?>> running = new ArrayList<>();
        for (Job<?, ?> job : Items.allItems2(ACL.SYSTEM2, Jenkins.get(), Job.class)) {
            for (Run<?, ?> run : candidateRuns(job)) {
                if (run != null && run.isBuilding()) {
                    running.add(run);
                }
            }
        }
        return running;
    }

    private static Collection<? extends Run<?, ?>> candidateRuns(Job<?, ?> job) {
        if (job instanceof LazyBuildMixIn.LazyLoadingJob<?, ?> lazy) {
            return lazy.getLazyBuildMixIn().getRunMap().getLoadedBuilds().values();
        }
        return job.getBuilds();
    }

    private void reset() {
        active = false;
        message = null;
        startedBy = null;
        since = 0;
    }

    @Restricted(NoExternalUse.class)
    void setRestarter(@NonNull Restarter restarter) {
        this.restarter = restarter;
    }
}
