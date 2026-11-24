# Safe Restarter

This plugin allows you to restart Jenkins safely.
Jenkins restarts once all running jobs are finished.

**Restart Safely** uses the core safe restart: no new builds start, builds that occupy an executor finish, and Jenkins restarts.
Pipeline builds are the exception.
They are paused at the next safe point and resumed after the restart, and they cannot start new `node` steps while Jenkins is preparing to shut down.
["Scaling Pipelines"](https://www.jenkins.io/doc/book/pipeline/scaling-pipeline/) provides an overview of Pipeline durability options.

## Restart after builds finish

**Restart After Builds Finish** waits for every running build instead, Pipelines included.
While it waits, new builds are held back in the queue, but running builds keep going: they can still start their remaining `node` steps and the builds they trigger themselves.
Once nothing is running, Jenkins restarts.
If nothing is running when you start it, Jenkins restarts right away.

The page lists the builds that still hold the restart and lets you cancel.
Administrators also see the pending restart in the notification bell and on the Manage Jenkins page.
A build that never finishes, for example one waiting on an `input` step, holds the restart until you cancel.

To make the existing **Restart Safely** links behave this way, enable **Let running Pipelines finish all their stages before restarting, instead of pausing and resuming them** under *Manage Jenkins » System » Safe Restart*.
The CLI `safe-restart` command, the REST endpoint and other plugins are not affected by this option.

## Side panel

![](docs/images/restart_side.png)

## Manage Jenkins page

![](docs/images/restart_admin.png)
