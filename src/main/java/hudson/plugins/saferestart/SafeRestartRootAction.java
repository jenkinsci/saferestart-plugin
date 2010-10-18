package hudson.plugins.saferestart;

import hudson.Extension;
import hudson.model.Hudson;
import hudson.model.RootAction;

/**
 * Action for SafeResatrt.
 * added restart link to left-side panel.
 * @author Seiji Sogabe
 */
@Extension
public class SafeRestartRootAction implements RootAction {

    public String getDisplayName() {
        return Messages.SafeRestartRootAction_displayName();
    }

    public String getIconFileName() {
        if (!hasAdministerPermission()) {
            return null;
        }
        return "/plugin/" + Contsants.ID + "/images/24x24/" + Contsants.ICON;
    }

    private boolean hasAdministerPermission() {
        return Hudson.getInstance().hasPermission(Hudson.ADMINISTER);
    }

    /**
     * {@inheritDoc}
     *
     * This actions always starts from the context directly,
     * so prefix {@link JobConfigHistoryConsts#URLNAME} with a slash.
     */
    public String getUrlName() {
        return Contsants.RESTART_URL;
    }
}
