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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import hudson.Functions;
import hudson.model.Action;
import hudson.model.User;
import hudson.security.ACL;
import hudson.security.ACLContext;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class SafeRestartRootActionTest {

    private static SafeRestartRootAction getRootAction(JenkinsRule j) {
        for (Action a : j.jenkins.getActions()) {
            if (a instanceof SafeRestartRootAction rootAction) {
                return rootAction;
            }
        }
        throw new AssertionError("SafeRestartRootAction not registered in Jenkins root actions");
    }

    @Test
    void testGetDisplayName(JenkinsRule j) {
        assertThat(getRootAction(j).getDisplayName(), is("Restart Safely"));
    }

    @Test
    void testGetIconFileName(JenkinsRule j) {
        SafeRestartRootAction action = getRootAction(j);
        if (Functions.isWindows()) {
            // WindowsLifecycle.canRestart() returns false outside a restartable Windows service
            assertThat(action.getIconFileName(), is(nullValue()));
        } else {
            assertThat(action.getIconFileName(), is("symbol-reload"));
        }
    }

    @Test
    void testGetIconFileNameReturnsNullWithoutAdministerPermission(JenkinsRule j) {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(
                new MockAuthorizationStrategy().grant(Jenkins.READ).everywhere().to("reader"));

        SafeRestartRootAction action = getRootAction(j);
        User reader = User.getOrCreateByIdOrFullName("reader");
        try (ACLContext ignored = ACL.as2(reader.impersonate2())) {
            assertThat(action.getIconFileName(), is(nullValue()));
        }
    }

    @Test
    void testGetUrlName(JenkinsRule j) {
        assertThat(getRootAction(j).getUrlName(), is("/safeRestart"));
    }
}
