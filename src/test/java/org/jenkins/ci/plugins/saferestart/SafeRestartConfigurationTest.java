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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class SafeRestartConfigurationTest {

    @Test
    void defaultIsOffAndLinksPointToCoreSafeRestart(JenkinsRule j) {
        assertFalse(SafeRestartConfiguration.get().isWaitForRunningBuilds());
        assertThat(new SafeRestartRootAction().getUrlName(), is(Constants.RESTART_URL));
        assertThat(new SafeRestartManagementLink().getUrlName(), is("safeRestart"));
        assertThat(
                new SafeRestartManagementLink().getDescription(), is(Messages.SafeRestartManagementLink_description()));
    }

    @Test
    void settingSurvivesConfigRoundtrip(JenkinsRule j) throws Exception {
        SafeRestartConfiguration.get().setWaitForRunningBuilds(true);
        j.configRoundtrip();
        assertTrue(SafeRestartConfiguration.get().isWaitForRunningBuilds());

        SafeRestartConfiguration.get().setWaitForRunningBuilds(false);
        j.configRoundtrip();
        assertFalse(SafeRestartConfiguration.get().isWaitForRunningBuilds());
    }

    @Test
    void linksPointToDrainPageWhenEnabled(JenkinsRule j) {
        SafeRestartConfiguration.get().setWaitForRunningBuilds(true);
        assertThat(new SafeRestartRootAction().getUrlName(), is(Constants.DRAIN_URL));
        assertThat(new SafeRestartManagementLink().getUrlName(), is(Constants.DRAIN_URL));
        assertThat(
                new SafeRestartManagementLink().getDescription(),
                is(Messages.SafeRestartManagementLink_descriptionDrain()));
    }
}
