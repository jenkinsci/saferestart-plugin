/*
 * The MIT License
 *
 * Copyright 2025 Mark Waite.
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
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkins.ci.plugins.saferestart;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.jenkins.ci.plugins.saferestart.RestartDrainTestSupport.installFakeRestarter;
import static org.jenkins.ci.plugins.saferestart.RestartDrainTestSupport.startBlocker;
import static org.jenkins.ci.plugins.saferestart.RestartDrainTestSupport.waitUntilHeldByDrain;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.queue.QueueTaskFuture;
import java.util.List;
import org.jenkins.ci.plugins.saferestart.RestartDrainTestSupport.BlockingBuilder;
import org.jenkins.ci.plugins.saferestart.RestartDrainTestSupport.FakeRestarter;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.test.steps.SemaphoreStep;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class RestartDrainTest {

    @Test
    void freestyleBuildIsHeldWhileDrainingAndReleasedOnCancel(JenkinsRule j) throws Exception {
        FakeRestarter restarter = installFakeRestarter();
        BlockingBuilder blocker = new BlockingBuilder();
        FreeStyleBuild blockerBuild = startBlocker(j.createFreeStyleProject("blocker"), blocker);

        RestartDrain drain = RestartDrain.get();
        drain.start("maintenance");
        assertTrue(drain.isActive());

        FreeStyleProject held = j.createFreeStyleProject("held");
        QueueTaskFuture<FreeStyleBuild> heldFuture = held.scheduleBuild2(0);
        waitUntilHeldByDrain(j, held);
        assertNull(held.getLastBuild(), "no build may start while draining");

        drain.cancel();
        assertFalse(drain.isActive());
        j.assertBuildStatusSuccess(heldFuture);

        blocker.release(blockerBuild.getNumber());
        j.assertBuildStatusSuccess(j.waitForCompletion(blockerBuild));
        assertEquals(0, restarter.calls.get(), "cancel must not restart");
    }

    @Test
    void pipelineAcquiresNextNodeWhileDrainingAndRestartFollowsCompletion(JenkinsRule j) throws Exception {
        FakeRestarter restarter = installFakeRestarter();
        WorkflowJob p = j.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition("node { semaphore 'a' }\nnode { semaphore 'b' }", true));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        SemaphoreStep.waitForStart("a/1", b);

        RestartDrain drain = RestartDrain.get();
        drain.start("maintenance");
        assertTrue(drain.isActive());
        assertEquals(List.of(b), drain.getRunningBuilds());

        SemaphoreStep.success("a/1", null);
        // The second node block needs a new executor; core quiet-down would never grant it.
        SemaphoreStep.waitForStart("b/1", b);
        assertTrue(drain.isActive(), "drain must stay active while the pipeline runs");
        assertEquals(0, restarter.calls.get());

        SemaphoreStep.success("b/1", null);
        j.assertBuildStatusSuccess(j.waitForCompletion(b));
        assertTrue(restarter.awaitCall(), "restart must follow the last build");
        assertEquals("maintenance", restarter.message.get());
        assertFalse(drain.isActive());
    }

    @Test
    void newPipelineBuildIsHeldWhileDraining(JenkinsRule j) throws Exception {
        FakeRestarter restarter = installFakeRestarter();
        BlockingBuilder blocker = new BlockingBuilder();
        FreeStyleBuild blockerBuild = startBlocker(j.createFreeStyleProject("blocker"), blocker);

        RestartDrain drain = RestartDrain.get();
        drain.start(null);

        WorkflowJob p = j.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition("node { }", true));
        QueueTaskFuture<WorkflowRun> future = p.scheduleBuild2(0);
        waitUntilHeldByDrain(j, p);
        assertNull(p.getLastBuild(), "no pipeline build may start while draining");

        drain.cancel();
        j.assertBuildStatusSuccess(future);

        blocker.release(blockerBuild.getNumber());
        j.assertBuildStatusSuccess(j.waitForCompletion(blockerBuild));
        assertEquals(0, restarter.calls.get());
    }

    @Test
    void drainHoldsUntilOlderConcurrentBuildFinishes(JenkinsRule j) throws Exception {
        FakeRestarter restarter = installFakeRestarter();
        FreeStyleProject p = j.createFreeStyleProject("concurrent");
        p.setConcurrentBuild(true);
        BlockingBuilder blocker = new BlockingBuilder();
        FreeStyleBuild first = startBlocker(p, blocker);
        FreeStyleBuild second = p.scheduleBuild2(0).waitForStart();
        blocker.awaitStarted(second.getNumber());

        RestartDrain drain = RestartDrain.get();
        drain.start(null);
        assertEquals(2, drain.getRunningBuilds().size());

        // The newer build finishes first; the older one must still hold the restart.
        blocker.release(second.getNumber());
        j.assertBuildStatusSuccess(j.waitForCompletion(second));
        drain.checkAndRestart();
        assertTrue(drain.isActive(), "older running build must hold the drain");
        assertEquals(0, restarter.calls.get());
        assertEquals(List.of(first), drain.getRunningBuilds());

        blocker.release(first.getNumber());
        j.assertBuildStatusSuccess(j.waitForCompletion(first));
        assertTrue(restarter.awaitCall());
        assertFalse(drain.isActive());
    }

    @Test
    void buildTriggeredByRunningBuildIsAllowed(JenkinsRule j) throws Exception {
        FakeRestarter restarter = installFakeRestarter();
        BlockingBuilder blocker = new BlockingBuilder();
        FreeStyleBuild upstream = startBlocker(j.createFreeStyleProject("upstream"), blocker);

        RestartDrain drain = RestartDrain.get();
        drain.start(null);

        FreeStyleProject downstream = j.createFreeStyleProject("downstream");
        QueueTaskFuture<FreeStyleBuild> downstreamFuture =
                downstream.scheduleBuild2(0, new CauseAction(new Cause.UpstreamCause(upstream)));
        j.assertBuildStatusSuccess(downstreamFuture);
        assertTrue(drain.isActive(), "a downstream build must not end the drain");

        FreeStyleProject unrelated = j.createFreeStyleProject("unrelated");
        QueueTaskFuture<FreeStyleBuild> unrelatedFuture = unrelated.scheduleBuild2(0);
        waitUntilHeldByDrain(j, unrelated);
        assertNull(unrelated.getLastBuild());

        blocker.release(upstream.getNumber());
        j.assertBuildStatusSuccess(j.waitForCompletion(upstream));
        assertTrue(restarter.awaitCall());
        assertFalse(drain.isActive());

        // With the fake restarter Jenkins does not quiet down, so the held build may now run.
        j.jenkins.getQueue().scheduleMaintenance();
        j.assertBuildStatusSuccess(unrelatedFuture);
    }

    @Test
    void startRestartsImmediatelyWhenNothingIsRunning(JenkinsRule j) throws Exception {
        FakeRestarter restarter = installFakeRestarter();
        RestartDrain drain = RestartDrain.get();
        assertThat(drain.getRunningBuilds(), empty());

        drain.start("  ");
        assertTrue(restarter.awaitCall());
        assertEquals(1, restarter.calls.get());
        assertNull(restarter.message.get(), "blank message must be passed as null");
        assertFalse(drain.isActive());
    }

    @Test
    void startIsIdempotentAndCancelWithoutDrainIsHarmless(JenkinsRule j) throws Exception {
        FakeRestarter restarter = installFakeRestarter();
        RestartDrain drain = RestartDrain.get();
        drain.cancel();
        assertFalse(drain.isActive());
        assertNull(drain.getMessage());
        assertNull(drain.getStartedBy());
        assertEquals(0, drain.getSince());

        BlockingBuilder blocker = new BlockingBuilder();
        FreeStyleBuild blockerBuild = startBlocker(j.createFreeStyleProject("blocker"), blocker);
        assertEquals(List.of(blockerBuild), drain.getRunningBuilds());

        drain.start("first");
        String startedBy = drain.getStartedBy();
        long since = drain.getSince();
        assertTrue(since > 0);
        drain.start("second");
        assertEquals("first", drain.getMessage(), "a second start must not replace the message");
        assertEquals(startedBy, drain.getStartedBy());
        assertEquals(since, drain.getSince());

        blocker.release(blockerBuild.getNumber());
        j.assertBuildStatusSuccess(j.waitForCompletion(blockerBuild));
        assertTrue(restarter.awaitCall());
        assertEquals("first", restarter.message.get());
        assertEquals(1, restarter.calls.get());
        assertThat(drain.getRunningBuilds(), empty());
        assertNull(drain.getMessage());
        assertNull(drain.getStartedBy());
        assertEquals(0, drain.getSince());
    }
}
