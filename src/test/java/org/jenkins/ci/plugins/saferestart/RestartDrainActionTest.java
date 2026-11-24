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
import static org.hamcrest.Matchers.is;
import static org.jenkins.ci.plugins.saferestart.RestartDrainTestSupport.installFakeRestarter;
import static org.jenkins.ci.plugins.saferestart.RestartDrainTestSupport.startBlocker;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.ExtensionList;
import hudson.model.FreeStyleBuild;
import hudson.model.ListView;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.htmlunit.HttpMethod;
import org.htmlunit.Page;
import org.htmlunit.WebRequest;
import org.htmlunit.html.HtmlAnchor;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.util.NameValuePair;
import org.jenkins.ci.plugins.saferestart.RestartDrainTestSupport.BlockingBuilder;
import org.jenkins.ci.plugins.saferestart.RestartDrainTestSupport.FakeRestarter;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Exercises the drain page over HTTP only, so it does not depend on the UI class names.
 */
@WithJenkins
class RestartDrainActionTest {

    @Test
    void pageRendersWhetherOrNotDraining(JenkinsRule j) throws Exception {
        installFakeRestarter();
        JenkinsRule.WebClient wc = j.createWebClient();
        HtmlPage idle = wc.goTo(Constants.DRAIN_URL);
        assertThat(idle.getWebResponse().getStatusCode(), is(200));

        BlockingBuilder blocker = new BlockingBuilder();
        FreeStyleBuild blockerBuild = startBlocker(j.createFreeStyleProject("blocker"), blocker);
        try {
            RestartDrain.get().start("maintenance");
            HtmlPage active = wc.goTo(Constants.DRAIN_URL);
            assertThat(active.getWebResponse().getStatusCode(), is(200));
            if (JenkinsUtils.canRestart()) {
                assertThat(
                        "the page lists the builds holding the restart",
                        active.asNormalizedText(),
                        containsString(blockerBuild.getFullDisplayName()));
            } else {
                // WindowsLifecycle.canRestart() returns false outside a restartable Windows service
                assertThat(
                        "the page explains Jenkins cannot restart on this platform",
                        active.asNormalizedText(),
                        containsString("Jenkins cannot restart itself on this platform."));
            }
        } finally {
            RestartDrain.get().cancel();
            blocker.release(blockerBuild.getNumber());
            j.assertBuildStatusSuccess(j.waitForCompletion(blockerBuild));
        }
    }

    @Test
    void startAndCancelOverHttp(JenkinsRule j) throws Exception {
        FakeRestarter restarter = installFakeRestarter();
        RestartDrainAction action = ExtensionList.lookupSingleton(RestartDrainAction.class);
        action.setCanRestart(() -> true);
        BlockingBuilder blocker = new BlockingBuilder();
        FreeStyleBuild blockerBuild = startBlocker(j.createFreeStyleProject("blocker"), blocker);
        try {
            JenkinsRule.WebClient wc = j.createWebClient();

            WebRequest start = new WebRequest(new URL(j.getURL(), Constants.DRAIN_URL + "/start"), HttpMethod.POST);
            wc.addCrumb(start);
            List<NameValuePair> params = new ArrayList<>(start.getRequestParameters());
            params.add(new NameValuePair("message", "maintenance"));
            start.setRequestParameters(params);
            Page afterStart = wc.getPage(start);
            assertThat(afterStart.getWebResponse().getStatusCode(), is(200));
            assertTrue(RestartDrain.get().isActive());
            assertEquals("maintenance", RestartDrain.get().getMessage());

            WebRequest cancel = new WebRequest(new URL(j.getURL(), Constants.DRAIN_URL + "/cancel"), HttpMethod.POST);
            wc.addCrumb(cancel);
            Page afterCancel = wc.getPage(cancel);
            assertThat(afterCancel.getWebResponse().getStatusCode(), is(200));
            assertFalse(RestartDrain.get().isActive());
        } finally {
            action.setCanRestart(JenkinsUtils::canRestart);
            RestartDrain.get().cancel();
            blocker.release(blockerBuild.getNumber());
            j.assertBuildStatusSuccess(j.waitForCompletion(blockerBuild));
        }
        assertEquals(0, restarter.calls.get());
    }

    @Test
    void startIsRejectedWhenJenkinsCannotRestart(JenkinsRule j) throws Exception {
        FakeRestarter restarter = installFakeRestarter();
        RestartDrainAction action = ExtensionList.lookupSingleton(RestartDrainAction.class);
        action.setCanRestart(() -> false);
        try {
            JenkinsRule.WebClient wc = j.createWebClient();
            wc.setThrowExceptionOnFailingStatusCode(false);

            WebRequest start = new WebRequest(new URL(j.getURL(), Constants.DRAIN_URL + "/start"), HttpMethod.POST);
            wc.addCrumb(start);
            Page response = wc.getPage(start);
            assertThat(response.getWebResponse().getStatusCode(), is(400));
            assertFalse(RestartDrain.get().isActive());
            assertEquals(0, restarter.calls.get());
        } finally {
            action.setCanRestart(JenkinsUtils::canRestart);
            RestartDrain.get().cancel();
        }
    }

    @Test
    void drainLinkResolvesFromSecondaryView(JenkinsRule j) throws Exception {
        installFakeRestarter();
        j.jenkins.addView(new ListView("secondary"));
        RestartDrainAction action = ExtensionList.lookupSingleton(RestartDrainAction.class);
        action.setCanRestart(() -> true);
        try {
            JenkinsRule.WebClient wc = j.createWebClient();
            HtmlPage viewPage = wc.goTo("view/secondary/");
            HtmlAnchor drainLink = viewPage.getAnchorByText(Messages.RestartDrainAction_displayName());
            HtmlPage drainPage = drainLink.click();
            assertThat(drainPage.getWebResponse().getStatusCode(), is(200));
            assertThat(drainPage.getUrl().getPath(), containsString("/" + Constants.DRAIN_URL));

            if (JenkinsUtils.canRestart()) {
                SafeRestartConfiguration.get().setWaitForRunningBuilds(true);
                HtmlPage viewPageDrainDefault = wc.goTo("view/secondary/");
                HtmlAnchor safeRestartLink =
                        viewPageDrainDefault.getAnchorByText(Messages.SafeRestartRootAction_displayName());
                HtmlPage safeRestartDrainPage = safeRestartLink.click();
                assertThat(safeRestartDrainPage.getWebResponse().getStatusCode(), is(200));
                assertThat(safeRestartDrainPage.getUrl().getPath(), containsString("/" + Constants.DRAIN_URL));
            }
        } finally {
            SafeRestartConfiguration.get().setWaitForRunningBuilds(false);
            action.setCanRestart(JenkinsUtils::canRestart);
        }
    }

    @Test
    void getRequestsCannotStartOrCancel(JenkinsRule j) throws Exception {
        installFakeRestarter();
        JenkinsRule.WebClient wc = j.createWebClient();
        wc.setThrowExceptionOnFailingStatusCode(false);

        Page start = wc.getPage(new WebRequest(new URL(j.getURL(), Constants.DRAIN_URL + "/start"), HttpMethod.GET));
        assertThat(start.getWebResponse().getStatusCode(), is(405));
        assertFalse(RestartDrain.get().isActive());

        Page cancel = wc.getPage(new WebRequest(new URL(j.getURL(), Constants.DRAIN_URL + "/cancel"), HttpMethod.GET));
        assertThat(cancel.getWebResponse().getStatusCode(), is(405));
    }
}
