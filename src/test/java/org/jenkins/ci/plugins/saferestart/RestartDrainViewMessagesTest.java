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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.jenkins.ci.plugins.saferestart.RestartDrainTestSupport.installFakeRestarter;
import static org.jenkins.ci.plugins.saferestart.RestartDrainTestSupport.startBlocker;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import hudson.model.FreeStyleBuild;
import hudson.security.ACL;
import java.util.regex.Pattern;
import org.jenkins.ci.plugins.saferestart.RestartDrainTestSupport.BlockingBuilder;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Checks that the drain views fill in their message arguments. Reads the response body only, so
 * the tests do not depend on where the core layout places the content.
 */
@WithJenkins
class RestartDrainViewMessagesTest {

    @Test
    @Issue("https://github.com/jenkinsci/saferestart-plugin/issues/87")
    void drainPageShowsWhoStartedTheDrainAndWhen(JenkinsRule j) throws Exception {
        // WindowsLifecycle.canRestart() returns false outside a restartable Windows service
        assumeTrue(JenkinsUtils.canRestart(), "the page shows the drain status only where Jenkins can restart");

        String body = bodyWhileDraining(j, Constants.DRAIN_URL);

        Pattern statusLine = Pattern.compile(
                ".*Started [^<{}]+ ago by " + Pattern.quote(ACL.SYSTEM2.getName()) + "\\..*", Pattern.DOTALL);
        assertThat("the status line names the time span and the user", body, matchesPattern(statusLine));
    }

    @Test
    @Issue("https://github.com/jenkinsci/saferestart-plugin/issues/87")
    void monitorShowsHowManyBuildsAreStillRunning(JenkinsRule j) throws Exception {
        String body = bodyWhileDraining(j, "manage");

        assertThat(
                "the monitor counts the builds holding the restart",
                body,
                containsString("Jenkins will restart once running builds finish. Still running: 1."));
    }

    /**
     * @return the response body of {@code path}, requested while one build holds the drain
     */
    private static String bodyWhileDraining(JenkinsRule j, String path) throws Exception {
        installFakeRestarter();
        BlockingBuilder blocker = new BlockingBuilder();
        FreeStyleBuild blockerBuild = startBlocker(j.createFreeStyleProject("blocker"), blocker);
        try {
            RestartDrain.get().start(null);
            return j.createWebClient().goTo(path).getWebResponse().getContentAsString();
        } finally {
            RestartDrain.get().cancel();
            blocker.release(blockerBuild.getNumber());
            j.assertBuildStatusSuccess(j.waitForCompletion(blockerBuild));
        }
    }
}
