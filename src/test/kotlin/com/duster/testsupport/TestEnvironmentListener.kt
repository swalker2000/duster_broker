package com.duster.testsupport

import org.junit.platform.launcher.LauncherSession
import org.junit.platform.launcher.LauncherSessionListener

/**
 * Работает в IDE/раннерах, которые используют JUnit Platform Launcher.
 * Для Gradle дополнительно включено автодетектирование JUnit Jupiter extension.
 */
class TestEnvironmentListener : LauncherSessionListener {
    override fun launcherSessionOpened(session: LauncherSession) {
        TestEnvironment.startIfNeeded()
    }

    override fun launcherSessionClosed(session: LauncherSession) {
        TestEnvironment.stop()
    }
}

