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

package com.nagopy.android.overlayviewmanager.sample

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import com.nagopy.android.overlayviewmanager.OverlaySpec
import com.nagopy.android.overlayviewmanager.OverlayState
import com.nagopy.android.overlayviewmanager.OverlayTouchMode
import com.nagopy.android.overlayviewmanager.OverlayView
import com.nagopy.android.overlayviewmanager.OverlayViewManager
import timber.log.Timber

/**
 * Activity-scoped overlay written in Kotlin. It is bounded by this Activity's window, so it needs
 * no "display over other apps" permission, and it is draggable while still delivering ordinary
 * clicks to the View.
 */
class Sample1Activity : BaseSampleWithCodeActivity() {

    /** Owned by this Activity; exposed read-only for the instrumentation test. */
    lateinit var overlayView: OverlayView<TextView>
        private set

    private var clickCounter = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample1)

        val textView = createTextView()
        // Click handling stays on the View itself, even with OverlayTouchMode.DRAGGABLE.
        textView.setOnClickListener {
            clickCounter++
            textView.text = "click:$clickCounter"
        }
        overlayView = OverlayViewManager.getInstance().newOverlayView(
            textView,
            this,
            OverlaySpec(touchMode = OverlayTouchMode.DRAGGABLE),
        )
    }

    /** Bound from the layout through `android:onClick`. */
    fun onClick(view: View) {
        val result = if (overlayView.state == OverlayState.ATTACHED) overlayView.hide() else overlayView.show()
        if (!result.isSuccess) {
            Timber.w(result.cause, "Sample1 show()/hide() failed: %s", result.failure)
        }
    }

    override fun onDestroy() {
        // The library also disposes activity-scoped handles when the Activity is destroyed;
        // disposing here keeps ownership explicit, and a repeated dispose() is a harmless no-op.
        val disposed = overlayView.dispose()
        if (!disposed.isSuccess) {
            Timber.w(disposed.cause, "Sample1 dispose() failed: %s", disposed.failure)
        }
        super.onDestroy()
    }

    private fun createTextView(): TextView = TextView(this).apply {
        id = R.id.sample_text_view
        TextViewCompat.setTextAppearance(this, androidx.appcompat.R.style.TextAppearance_AppCompat_Large)
        text = "click:$clickCounter"
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.RED)
        setPadding(10, 10, 10, 10)
    }
}
