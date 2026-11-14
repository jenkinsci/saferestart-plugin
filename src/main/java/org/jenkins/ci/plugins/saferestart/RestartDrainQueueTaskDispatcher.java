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
import hudson.model.Cause;
import hudson.model.Job;
import hudson.model.Queue;
import hudson.model.Run;
import hudson.model.queue.CauseOfBlockage;
import hudson.model.queue.QueueTaskDispatcher;
import hudson.security.ACL;
import hudson.security.ACLContext;

/**
 * Holds back new top-level builds while {@link RestartDrain} is active.
 *
 * <p>Everything else goes through so running builds can finish: Pipeline {@code node} steps and
 * other sub-tasks, tasks core itself exempts from quiet-down, and builds triggered by a build that
 * is still running.
 */
@Extension
public class RestartDrainQueueTaskDispatcher extends QueueTaskDispatcher {

    @Override
    @CheckForNull
    public CauseOfBlockage canRun(Queue.Item item) {
        if (!RestartDrain.get().isActive()) {
            return null;
        }
        return blocksNewBuild(item) ? CauseOfBlockage.fromMessage(Messages._RestartDrain_blocked()) : null;
    }

    /**
     * @return true if the item is a new top-level build that must wait for the restart
     */
    static boolean blocksNewBuild(Queue.Item item) {
        Queue.Task task = item.task;
        if (!(task instanceof Job)) {
            return false;
        }
        if (task instanceof Queue.NonBlockingTask) {
            return false;
        }
        try (ACLContext ignored = ACL.as2(ACL.SYSTEM2)) {
            for (Cause cause : item.getCauses()) {
                if (cause instanceof Cause.UpstreamCause upstream) {
                    Run<?, ?> upstreamRun = upstream.getUpstreamRun();
                    if (upstreamRun != null && upstreamRun.isBuilding()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
