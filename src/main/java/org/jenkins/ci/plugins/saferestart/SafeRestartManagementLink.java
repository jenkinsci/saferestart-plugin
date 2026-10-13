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

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.ManagementLink;
import hudson.model.ManagementLink.Category;

/**
 * ManagementLink for SafeRestart. Added restart link to system administrator.
 *
 * @author Seiji Sogabe
 * @author <a href="mailto:jieryn@gmail.com">Jesse Farinacci</a>
 */
@Extension
public class SafeRestartManagementLink extends ManagementLink {

    @Override
    public String getDescription() {
        return Messages.SafeRestartManagementLink_description();
    }

    @Override
    public String getDisplayName() {
        return Messages.SafeRestartManagementLink_displayName();
    }

    @Override
    public String getIconFileName() {
        return Constants.ICON;
    }

    @Override
    public String getUrlName() {
        // ManagementLink.getUrlName() is resolved by core relative to the Jenkins root, so it
        // must not include the context path (unlike Constants.RESTART_URL, used by
        // SafeRestartRootAction, which is intentionally context-path-relative).
        return Constants.RESTART_URL.substring(1);
    }

    @Override
    @NonNull
    public Category getCategory() {
        return Category.TOOLS;
    }
}
