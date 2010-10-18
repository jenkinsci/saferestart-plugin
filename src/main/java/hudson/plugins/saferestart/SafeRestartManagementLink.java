package hudson.plugins.saferestart;

import hudson.Extension;
import hudson.model.Hudson;
import hudson.model.ManagementLink;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;

/**
 * ManagementLink for SafeRestart.
 * added restrat link to system admin.
 * @author Seiji Sogabe
 */
@Extension
public class SafeRestartManagementLink extends ManagementLink {

    @Override
    public String getDescription() {
        return Messages.SafeRestartManagementLink_description();
    }

    @Override
    public String getIconFileName() {
        return "/plugin/" + Contsants.ID + "/images/48x48/" + Contsants.ICON;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * In case of {@link ManagementLink}, this value is put straight into
     * the href attribute,
     * so relative paths are interpreted against the root {@link Hudson} object.
     */
    @Override
    public String getUrlName() {
        final StaplerRequest req = Stapler.getCurrentRequest();
        if (req != null) {
            return req.getContextPath() + Contsants.RESTART_URL;
        }
        return Contsants.RESTART_URL;
    }

    public String getDisplayName() {
        return Messages.SafeRestartManagementLink_displayName();
    }
}
