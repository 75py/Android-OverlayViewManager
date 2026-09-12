package com.nagopy.android.overlayviewmanager.lint;

import org.junit.Test;

import static com.android.tools.lint.detector.api.ApiKt.CURRENT_API;
import static com.nagopy.android.overlayviewmanager.lint.SetOverlayAboveNavigationViewsDetector.ALLOW_VIEW_TO_EXTEND_OUTSIDE_SCREEN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.android.tools.lint.client.api.LintClient;
import com.android.tools.lint.client.api.Vendor;

public class IssueRegistryTest {

    @Test
    public void getIssues() throws Exception {
        LintClient.clientName = "LintClient.clientName";
        assertTrue(new IssueRegistry().getIssues().contains(ALLOW_VIEW_TO_EXTEND_OUTSIDE_SCREEN));
    }

    @Test
    public void getApi_matchesCurrentLintApi() throws Exception {
        assertEquals(CURRENT_API, new IssueRegistry().getApi());
    }

    @Test
    public void getVendor_isDeclaredWithFeedbackUrl() throws Exception {
        Vendor vendor = new IssueRegistry().getVendor();
        assertNotNull(vendor);
        assertNotNull(vendor.getVendorName());
        assertNotNull(vendor.getFeedbackUrl());
    }

}