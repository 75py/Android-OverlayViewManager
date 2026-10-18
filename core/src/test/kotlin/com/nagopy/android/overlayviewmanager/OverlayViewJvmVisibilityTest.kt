/*
 * Copyright 2026 75py
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

package com.nagopy.android.overlayviewmanager

import java.lang.reflect.Member
import java.lang.reflect.Modifier
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the Java-visible surface of [OverlayView]. Kotlin `internal` members compile to public
 * JVM members, so library-only entry points must be private or synthetic to stay out of reach of
 * Java sources.
 */
class OverlayViewJvmVisibilityTest {
    @Test
    fun constructors_areNotCallableFromJavaSources() {
        val callable = OverlayView::class.java.declaredConstructors.filter(::isCallableFromJava)

        assertTrue("Java-callable constructors: $callable", callable.isEmpty())
    }

    @Test
    fun libraryOnlyMembers_areNotCallableFromJavaSources() {
        val libraryOnly = listOf("create", "disposeForActivityDestruction")
        val members = (OverlayView::class.java.declaredMethods + OverlayView.Companion::class.java.declaredMethods)
            .filter { method -> libraryOnly.any { method.name == it || method.name.startsWith(it + "$") } }

        assertTrue("Expected the factory and the forced-disposal hook to exist", members.size >= libraryOnly.size)
        val callable = members.filter(::isCallableFromJava)
        assertTrue("Java-callable library-only members: $callable", callable.isEmpty())
    }

    private fun isCallableFromJava(member: Member): Boolean =
        !member.isSynthetic && !Modifier.isPrivate(member.modifiers)
}
