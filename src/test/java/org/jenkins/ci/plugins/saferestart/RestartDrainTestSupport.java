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

import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Queue;
import hudson.model.queue.CauseOfBlockage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;

/**
 * Shared helpers for the drain mode tests.
 */
final class RestartDrainTestSupport {

    private static final long TIMEOUT_SECONDS = 30;

    private RestartDrainTestSupport() {}

    /**
     * Builder that blocks each build until the test releases it, keyed by build number so
     * concurrent builds of one project can be released independently.
     */
    static final class BlockingBuilder extends TestBuilder {
        private final Map<Integer, CountDownLatch> started = new ConcurrentHashMap<>();
        private final Map<Integer, CountDownLatch> released = new ConcurrentHashMap<>();

        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                throws InterruptedException {
            int number = build.getNumber();
            startedLatch(number).countDown();
            return releasedLatch(number).await(2, TimeUnit.MINUTES);
        }

        void awaitStarted(int number) throws InterruptedException {
            assertTrue(
                    startedLatch(number).await(TIMEOUT_SECONDS, TimeUnit.SECONDS),
                    "build #" + number + " did not start");
        }

        void release(int number) {
            releasedLatch(number).countDown();
        }

        private CountDownLatch startedLatch(int number) {
            return started.computeIfAbsent(number, k -> new CountDownLatch(1));
        }

        private CountDownLatch releasedLatch(int number) {
            return released.computeIfAbsent(number, k -> new CountDownLatch(1));
        }
    }

    /**
     * Records restart requests instead of restarting the test JVM.
     */
    static final class FakeRestarter implements RestartDrain.Restarter {
        final AtomicInteger calls = new AtomicInteger();
        final AtomicReference<String> message = new AtomicReference<>();
        private final CountDownLatch called = new CountDownLatch(1);

        @Override
        public void restart(String message) {
            this.message.set(message);
            calls.incrementAndGet();
            called.countDown();
        }

        boolean awaitCall() throws InterruptedException {
            return called.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
    }

    /**
     * Must be called before {@link RestartDrain#start} in every test.
     */
    static FakeRestarter installFakeRestarter() {
        FakeRestarter restarter = new FakeRestarter();
        RestartDrain.get().setRestarter(restarter);
        return restarter;
    }

    /**
     * Schedules a build of the project that blocks until {@link BlockingBuilder#release} and
     * returns once it is running.
     */
    static FreeStyleBuild startBlocker(FreeStyleProject project, BlockingBuilder builder) throws Exception {
        project.getBuildersList().add(builder);
        FreeStyleBuild build = project.scheduleBuild2(0).waitForStart();
        builder.awaitStarted(build.getNumber());
        return build;
    }

    static void waitUntil(BooleanSupplier condition, String description) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("Timed out waiting for " + description);
            }
            Thread.sleep(100);
        }
    }

    /**
     * True once the project's queue item is held back by the drain.
     */
    static boolean isHeldByDrain(JenkinsRule j, Queue.Task task) {
        Queue.Item item = j.jenkins.getQueue().getItem(task);
        if (item == null) {
            return false;
        }
        CauseOfBlockage cause = item.getCauseOfBlockage();
        return cause != null && Messages.RestartDrain_blocked().equals(cause.getShortDescription());
    }

    static void waitUntilHeldByDrain(JenkinsRule j, Queue.Task task) throws InterruptedException {
        waitUntil(() -> isHeldByDrain(j, task), task.getFullDisplayName() + " to be held by the drain");
    }
}
