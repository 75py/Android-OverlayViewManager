/*
 * Copyright 2017 75py
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nagopy.android.overlayviewmanager.internal;

import androidx.annotation.RestrictTo;
import android.util.Log;

import com.nagopy.android.overlayviewmanager.BuildConfig;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

/**
 * Inner class for debug.
 */
@RestrictTo(LIBRARY)
public class Logger {

    static final String TAG = "OverlayViewManager";
    static final boolean OUTPUT = BuildConfig.DEBUG;

    private Logger() {
    }

    static String createTag() {
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        if (stackTrace.length <= 2) {
            return TAG;
        } else {
            StackTraceElement caller = stackTrace[2];
            String className = caller.getClassName();
            String simpleClassName = className.substring(className.lastIndexOf('.') + 1);
            String methodName = caller.getMethodName();
            return simpleClassName + '#' + methodName;
        }
    }

    static String createCallerLink() {
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        if (stackTrace.length <= 2) {
            return "";
        } else {
            StackTraceElement caller = stackTrace[2];
            String fileName = caller.getFileName();
            int lineNumber = caller.getLineNumber();
            return '(' + fileName + ':' + lineNumber + ") ";
        }
    }

    public static void v(String msg, Object... args) {
        if (OUTPUT) {
            Log.v(createTag(), String.format(createCallerLink() + msg, args));
        }
    }

    public static void v(Throwable t, String msg, Object... args) {
        if (OUTPUT) {
            Log.v(createTag(), String.format(createCallerLink() + msg, args), t);
        }
    }

    public static void d(String msg, Object... args) {
        if (OUTPUT) {
            Log.d(createTag(), String.format(createCallerLink() + msg, args));
        }
    }

    public static void w(String msg, Object... args) {
        if (OUTPUT) {
            Log.w(createTag(), String.format(createCallerLink() + msg, args));
        }
    }

    public static void w(Throwable t, String msg, Object... args) {
        if (OUTPUT) {
            Log.w(createTag(), String.format(createCallerLink() + msg, args), t);
        }
    }

    public static void e(Throwable t, String msg, Object... args) {
        if (OUTPUT) {
            Log.e(createTag(), String.format(createCallerLink() + msg, args), t);
        }
    }

}
