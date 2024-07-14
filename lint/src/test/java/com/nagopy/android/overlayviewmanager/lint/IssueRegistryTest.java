package com.nagopy.android.overlayviewmanager.lint;

import org.junit.Test;

import static com.nagopy.android.overlayviewmanager.lint.SetOverlayAboveNavigationViewsDetector.ALLOW_VIEW_TO_EXTEND_OUTSIDE_SCREEN;
import static org.junit.Assert.assertTrue;

import com.android.tools.lint.client.api.LintClient;

public class IssueRegistryTest {

    @Test
    public void getIssues() throws Exception {
        LintClient.clientName = "LintClient.clientName";
        assertTrue(new IssueRegistry().getIssues().contains(ALLOW_VIEW_TO_EXTEND_OUTSIDE_SCREEN));
    }

}