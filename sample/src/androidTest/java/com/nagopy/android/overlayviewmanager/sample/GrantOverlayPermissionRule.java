package com.nagopy.android.overlayviewmanager.sample;

import android.app.UiAutomation;
import android.os.ParcelFileDescriptor;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Enables SYSTEM_ALERT_WINDOW for the test package via the appops shell command.
 * GrantPermissionRule cannot grant it because "display over other apps" is an app-op
 * rather than a runtime permission -- in particular `pm grant` fails on API 23.
 */
class GrantOverlayPermissionRule implements TestRule {

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                UiAutomation automation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
                String pkg = InstrumentationRegistry.getInstrumentation()
                        .getTargetContext().getPackageName();
                try (ParcelFileDescriptor pfd = automation.executeShellCommand(
                        "appops set " + pkg + " SYSTEM_ALERT_WINDOW allow");
                     BufferedReader reader = new BufferedReader(new InputStreamReader(
                             new ParcelFileDescriptor.AutoCloseInputStream(pfd)))) {
                    while (reader.readLine() != null) {
                        // drain output
                    }
                }
                base.evaluate();
            }
        };
    }
}
