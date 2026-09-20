package com.nagopy.android.overlayviewmanager.lint;

import org.junit.Before;
import org.junit.Test;

import static com.android.tools.lint.detector.api.ApiKt.CURRENT_API;
import static com.nagopy.android.overlayviewmanager.lint.SetOverlayAboveNavigationViewsDetector.ALLOW_OUTSIDE_BOUNDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.android.tools.lint.client.api.LintClient;
import com.android.tools.lint.client.api.Vendor;
import com.android.tools.lint.detector.api.Issue;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class IssueRegistryTest {

    @Before
    public void setUp() throws Exception {
        // IssueRegistry's constructor requires LintClient.clientName to be initialized.
        LintClient.clientName = "IssueRegistryTest";
    }

    @Test
    public void getIssues() throws Exception {
        assertTrue(new IssueRegistry().getIssues().contains(ALLOW_OUTSIDE_BOUNDS));
    }

    @Test
    public void getApi_declaresAnApiLintCanLoad() throws Exception {
        int api = new IssueRegistry().getApi();
        // lint refuses to load a registry whose api exceeds the runtime's CURRENT_API,
        // and a nonpositive value is meaningless -- assert the contract itself rather
        // than only comparing the same constant to itself.
        assertTrue(api > 0);
        assertTrue(api <= CURRENT_API);
        // The registry builds against lint-api 32.2.1 (CURRENT_API == 16); a deliberate
        // dependency bump must update this expectation.
        assertEquals(16, api);
        assertEquals(CURRENT_API, api);
    }

    @Test
    public void getVendor_declaresExactCoordinates() throws Exception {
        Vendor vendor = new IssueRegistry().getVendor();
        assertNotNull(vendor);
        assertEquals("OverlayViewManager", vendor.getVendorName());
        assertEquals("com.nagopy.android:overlayviewmanager-lint", vendor.getIdentifier());
        assertEquals("https://github.com/75py/Android-OverlayViewManager/issues", vendor.getFeedbackUrl());
    }

    @Test
    public void registryLoadsThroughLintRegistryV2Manifest() throws Exception {
        // Emulate the consumer-side loading path: a jar carrying the Lint-Registry-v2
        // manifest attribute must name a class that loads and instantiates cleanly.
        String registryClass = IssueRegistry.class.getName();
        URL location = IssueRegistry.class.getResource("IssueRegistry.class");
        assertNotNull(location);
        assertEquals("file", location.getProtocol());
        File classFile = new File(location.toURI());

        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().putValue("Lint-Registry-v2", registryClass);

        File jar = File.createTempFile("overlayviewmanager-lint", ".jar");
        jar.deleteOnExit();
        File packageDir = classFile.getParentFile();
        File[] lintClasses = packageDir.listFiles((dir, name) -> name.endsWith(".class"));
        assertNotNull(lintClasses);
        try (JarOutputStream out = new JarOutputStream(new FileOutputStream(jar), manifest)) {
            for (File entry : lintClasses) {
                out.putNextEntry(new JarEntry(
                        registryClass.substring(0, registryClass.lastIndexOf('.') + 1).replace('.', '/')
                                + entry.getName()));
                java.nio.file.Files.copy(entry.toPath(), out);
                out.closeEntry();
            }
        }

        // Load through a separate class loader, as lint does for bundled lint jars.
        // This module's own classes resolve child-first so the jar copies are used.
        try (URLClassLoader loader = new URLClassLoader(new URL[]{jar.toURI().toURL()},
                IssueRegistryTest.class.getClassLoader()) {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                if (name.startsWith("com.nagopy.android.overlayviewmanager.lint.")) {
                    return findClass(name);
                }
                return super.loadClass(name);
            }
        }) {
            Class<?> loaded = loader.loadClass(registryClass);
            assertTrue(com.android.tools.lint.client.api.IssueRegistry.class.isAssignableFrom(loaded));
            Object instance = loaded.getDeclaredConstructor().newInstance();
            int api = (Integer) loaded.getMethod("getApi").invoke(instance);
            assertTrue(api > 0);
            assertTrue(api <= CURRENT_API);
            @SuppressWarnings("unchecked")
            List<Issue> issues = (List<Issue>) loaded.getMethod("getIssues").invoke(instance);
            assertEquals(1, issues.size());
        }
    }

}
