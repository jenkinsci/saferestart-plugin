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

import hudson.model.ManagementLink;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class SafeRestartManagementLinkTest {

    private static JenkinsRule j;

    private SafeRestartManagementLink managementLink;

    @BeforeAll
    static void setUp(JenkinsRule rule) {
        j = rule;
    }

    @BeforeEach
    void createManagementLink() {
        managementLink = new SafeRestartManagementLink();
    }

    @Test
    void testGetUrlName_StaplerRequest2() {
        assertThat(SafeRestartManagementLink.getUrlName(null), is(Constants.RESTART_URL));
    }

    @Test
    void testGetDescription() {
        assertThat(managementLink.getDescription(), is("Restart once no jobs are running."));
    }

    @Test
    void testGetDisplayName() {
        assertThat(managementLink.getDisplayName(), is("Restart Safely"));
    }

    @Test
    void testGetIconFileName() {
        assertThat(managementLink.getIconFileName(), is("symbol-reload"));
    }

    @Test
    void testGetUrlName() {
        assertThat(managementLink.getUrlName(), is("/safeRestart"));
    }

    @Test
    void testGetCategory() {
        assertThat(managementLink.getCategory(), is(ManagementLink.Category.TOOLS));
    }
}
