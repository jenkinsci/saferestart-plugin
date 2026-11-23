# AGENTS.md — Jenkins Safe Restart Plugin (`saferestart`)

## Overview
- **Plugin ID**: `saferestart`
- **Repository**: `jenkinsci/saferestart-plugin`
- **Java Package**: `org.jenkins.ci.plugins.saferestart`
- **Jenkins Baseline**: `2.479.1` (`org.jenkins-ci.plugins:plugin:6.x`)

Provides a **Restart Safely** link in the Jenkins side panel (`SafeRestartRootAction`) and **Manage Jenkins -> Tools and Actions** (`SafeRestartManagementLink`), delegating to Jenkins core's `/safeRestart` endpoint so Jenkins restarts once running builds finish.

---

## Build, Test & Verification Commands

### Prerequisites
- **JDK**: Java 17, 21, or 25
- **Maven**: Apache Maven `3.9+`

### Core Maven Commands
```bash
# Auto-format Java and POM files with Spotless
mvn spotless:apply

# Full build, JUnit 5 tests, InjectedTest, SpotBugs (Max effort), Spotless, and Enforcer checks
mvn clean verify

# Run unit tests quickly (skipping InjectedTest during development)
mvn test -Dtest=!InjectedTest

# Run SpotBugs and Maven Enforcer checks directly
mvn spotbugs:check enforcer:enforce
```

---

## Architecture & Guardrails

1. **URL Path Handling (`SafeRestartManagementLink` vs `SafeRestartRootAction`)**:
   - `ManagementLink.getUrlName()` is resolved by Jenkins core relative to the Jenkins root and must **not** include a leading `/` (`"safeRestart"`, avoiding duplicate context paths as fixed in #79).
   - `RootAction.getUrlName()` uses `Constants.RESTART_URL` (`"/safeRestart"`).
2. **Platform Lifecycle Behavior**:
   - `Jenkins.get().getLifecycle().canRestart()` returns `false` on Windows when Jenkins is not running under a restartable Windows service wrapper, causing `SafeRestartRootAction.getIconFileName()` to return `null` on Windows in `JenkinsRule` tests.
3. **Formatting & Static Analysis**:
   - `spotless.check.skip=false`, `spotbugs.effort=Max`, `spotbugs.threshold=Low`, and `ban-junit4-imports.skip=false` are enforced. Always run `mvn spotless:apply clean verify` before pushing.
