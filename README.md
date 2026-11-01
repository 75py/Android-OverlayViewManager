# OverlayViewManager

OverlayViewManager manages Android overlay views with explicit configuration, synchronous operation results, and Activity or application ownership. Java and Kotlin callers use the same API.

This branch documents **3.0.0 (unreleased)**. For an existing 2.x application, start with the [migration guide](docs/migration/2.x-to-3.0.md). It has not been published yet, so do not assume a 3.0.0 artifact is available.

## Requirements and installation

The library supports Android API 23 and later. This repository builds with JDK 17, Gradle 9.4.1, AGP 9.2.1, and Android SDK 36. Its Android modules use AGP's built-in Kotlin support.

Add JitPack to your dependency repositories, then use an available release or commit version:

```groovy
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

```groovy
dependencies {
    implementation 'com.github.75py:Android-OverlayViewManager:<version>'
}
```

The JitPack build of this coordinate can only be checked once a 3.0.0 tag exists. See the [sample project](sample/) for source dependencies during development.

## Initialize once per process

Call `OverlayViewManager.init()` from your manifest-registered `Application.onCreate()` on the main thread. Repeating it with the same Application is harmless; using a different Application in the same process throws.

```java
public final class MyApplication extends android.app.Application {
    @Override public void onCreate() {
        super.onCreate();
        OverlayViewManager.init(this);
    }
}
```

## Choose an owner

| Scope | Factory | Lifetime and permission |
| --- | --- | --- |
| Activity | `newOverlayView(view, activity, spec)` | Attached to the Activity window; no draw-over-other-apps permission. Explicitly dispose when finished. Activity destruction is a cleanup safety net. |
| Application | `newOverlayView(view, spec)` | Can appear above other apps while permitted. Keep a process- or service-owned handle and dispose it explicitly. The library does not start a service or keep the process alive. |

Create each managed View without an existing parent. For an application overlay, use an application/service context rather than retaining an Activity through its View.

## Activity overlay in Kotlin

In an Activity, keep the handle as a field. `OverlaySpec` is immutable; use Kotlin defaults and `copy()` for updates.

```kotlin
private var overlay: OverlayView<TextView>? = null

private fun showMessage() {
    val handle = overlay ?: OverlayViewManager.getInstance().newOverlayView(
        TextView(this).apply { text = "Drag me" },
        this,
        OverlaySpec(touchMode = OverlayTouchMode.DRAGGABLE, x = 24, y = 48),
    ).also { overlay = it }
    handle.view.setOnClickListener {
        Toast.makeText(this, "Overlay clicked", Toast.LENGTH_SHORT).show()
    }
    report(handle.show())
}

private fun moveMessage() {
    val handle = overlay ?: return
    report(handle.update(handle.spec.copy(x = 80, y = 120)))
}

private fun closeMessage() {
    val handle = overlay ?: return
    val result = handle.dispose()
    report(result)
    if (result.isSuccess) overlay = null
}

private fun report(result: OverlayResult) {
    if (!result.isSuccess) Log.w("Overlay", "Operation failed: ${result.failure}", result.cause)
}

override fun onDestroy() {
    closeMessage()
    super.onDestroy()
}
```

These members belong to an Activity; imports are Android `TextView`, `Toast`, `Log` and the library types. A failed explicit disposal retains the handle for a retry while the owner is alive. When an Activity is destroyed, the library makes one final removal attempt and releases its Activity-owned references even if removal fails. This safety net is different from retryable explicit disposal.

## Java configuration and results

Java callers use `OverlaySpec.Builder` and `toBuilder()`. In the owner of an existing handle:

```java
OverlaySpec next = overlay.getSpec().toBuilder()
        .setTouchMode(OverlayTouchMode.INTERACTIVE)
        .setAlpha(0.8f)
        .setX(80)
        .setY(120)
        .build();
OverlayResult result = overlay.update(next);
if (!result.isSuccess()) {
    Log.w("Overlay", "Update failed: " + result.getFailure(), result.getCause());
}
```

`show()`, `update(spec)`, `hide()` and `dispose()` return `OverlayResult`; they no longer form a fluent chain. Inspect `isSuccess`, `failure`, `state` and `changed`. `ATTACHED` reports the handle's attachment state, not a guarantee that Android or another window is letting the user see it. Invalid arguments and off-main-thread use are programming errors and can still throw.

`hide()` allows later reuse. Successful `dispose()` releases the View; do not read `view` or call `show()` afterward. Keep the sole application-overlay handle when disposal fails, so a host action can retry. See the [application ownership example](docs/migration/2.x-to-3.0.md#application-overlay-and-permission-return).

## Permission belongs to the host

Application overlays require the manifest declaration:

```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

Use `OverlayPermission.isGranted(context)` before a user-requested show. Launch `settingsIntent(context)` through the host's Activity Result launcher, then query `isGranted` again on return; the result code alone is not permission evidence. Remember whether the user requested a show, consume that request once, and never reopen Settings or retry automatically on denial. The library does not launch permission UI. The [migration guide](docs/migration/2.x-to-3.0.md#application-overlay-and-permission-return) shows the full flow.

A grant can be revoked after the check. Always inspect the subsequent operation result. Activity overlays do not need this permission flow.

## Touch, positioning, and brightness

- `PASS_THROUGH` does not receive touches; `INTERACTIVE` receives View interactions; `DRAGGABLE` adds library drag handling. Install click listeners on the managed View.
- `CrossUidPassThrough.SAME_UID_ONLY` is the default. Opt into `WHEN_SYSTEM_ALLOWS` only when passing touches to another app is required. Android's opacity and window policies can reject this with `PASS_THROUGH_OPACITY_EXCEEDED` or `PASS_THROUGH_UNSUPPORTED`; the library cannot guarantee another app receives the touch. `SAME_UID_ONLY` does not isolate touches by UID; Android decides. For a pass-through application overlay above the maximum obscuring opacity (0.8 by default), the outcome depends on whether the build includes the window manager's opacity clamp. A build with the clamp lowers the overlay to that value and passes the touch through, and a build without it keeps the overlay opaque and blocks the touch. For example, the API 36 emulator image has the clamp, while the API 31 emulator images, including the initial Android 12 release, do not. Overlapping overlays can exceed the combined limit either way.
- Geometry uses the owning window's bounds and insets. Obtain slider limits from the host window, with a compatible fallback on older supported APIs. `allowOutsideBounds` relaxes normal bounds handling; it does not guarantee placement over system bars or other restricted areas. A drag normalizes `spec` to top-left offsets without margins; with `allowOutsideBounds` it keeps the gravity and margins and only changes `x` and `y`.
- `screenBrightness` is an optional Activity-scope setting. Supplying it to an application overlay is an argument error.

Factories, operations, and `view`/`spec` access belong on the main thread. Only `state` and `lastFailure` are thread-safe handle reads. `OverlayPermission` is immutable and thread-safe.

## Development and limits

See [the 3.0.0 migration guide](docs/migration/2.x-to-3.0.md) and these sample sources:

- [Kotlin Activity overlay](sample/src/main/kotlin/com/nagopy/android/overlayviewmanager/sample/Sample1Activity.kt).
- [Java permission recheck](sample/src/main/java/com/nagopy/android/overlayviewmanager/sample/SampleStartActivity.java), without automatic showing on return.
- [Java application overlay owner](sample/src/main/java/com/nagopy/android/overlayviewmanager/sample/Sample2OverlayController.java) and [Service/retry UI](sample/src/main/java/com/nagopy/android/overlayviewmanager/sample/Sample2Activity.java).
- [Java spec updates and host-window bounds](sample/src/main/java/com/nagopy/android/overlayviewmanager/sample/SampleAllOptionsActivity.java).

 The library does not provide background execution, a foreground service, notifications, or automatic permission recovery. The host owns those responsibilities for long-running application overlays. OS and device window policies still apply.

Device tests ran on emulators at API 23, 26, 31, 35 and 36. Results and the exact builds are recorded in [V3_0_0_PLAN.md](V3_0_0_PLAN.md).

Known limits:

- With `allowOutsideBounds`, fill gravity changes the window's size during a drag instead of moving it.
- A layout direction declared in XML cannot be read before the first measure. If it differs from the configuration, an overlay using `START` or `END` can appear on the estimated side until its first layout.
- The bundled lint check inspects `OverlaySpec.Builder.setAllowOutsideBounds` only, not Kotlin constructor or `copy` arguments.

## License

Copyright 2017 75py. Licensed under the [Apache License, Version 2.0](LICENSE).
