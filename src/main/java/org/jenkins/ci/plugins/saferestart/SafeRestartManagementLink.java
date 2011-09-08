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

import hudson.Extension;
import hudson.model.ManagementLink;

import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;

/**
 * ManagementLink for SafeRestart. Added restart link to system administrator.
 * 
 * @author Seiji Sogabe
 */
@Extension
public class SafeRestartManagementLink extends ManagementLink {
  @Override
  public String getDescription() {
    return Messages.SafeRestartManagementLink_description();
  }

  public String getDisplayName() {
    return Messages.SafeRestartManagementLink_displayName();
  }

  @Override
  public String getIconFileName() {
    return "/plugin/" + Contsants.ID + "/images/48x48/" + Contsants.ICON;
  }

  @Override
  public String getUrlName() {
    final StaplerRequest req = Stapler.getCurrentRequest();
    if (req != null) {
      return req.getContextPath() + Contsants.RESTART_URL;
    }
    return Contsants.RESTART_URL;
  }
}
