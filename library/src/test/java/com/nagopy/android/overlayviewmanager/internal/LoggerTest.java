package com.nagopy.android.overlayviewmanager.internal;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LoggerTest {

    private String callCreateTag() {
        return Logger.createTag();
    }

    private String callCreateCallerLink() {
        return Logger.createCallerLink();
    }

    @Test
    public void createTag() {
        String tag = callCreateTag();
        assertEquals("LoggerTest#createTag", tag);
    }

    @Test
    public void createCallerLink() {
        String link = callCreateCallerLink();
        assertEquals("(LoggerTest.java:25) ", link);
    }

}