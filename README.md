# OverlayViewManager

OverlayViewManager manages Android overlay views with explicit configuration, synchronous operation results, and Activity or application ownership. Java and Kotlin callers use the same API.

This branch documents **3.0.0 (unreleased)**. For an existing 2.x application, start with the [migration guide](docs/migration/2.x-to-3.0.md). Release verification and publication are still pending; do not assume a published 3.0.0 artifact is available.

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
    implementation 'com.github.75py.Android-OverlayViewManager:overlayviewmanager:<version>'
    // Optional: the host also supplies Timber.
    implementation 'com.github.75py.Android-OverlayViewManager:overlayviewmanager-opt-timber:<version>'
    implementation 'com.jakewharton.timber:timber:5.0.1'
}
```

The final 3.0.0 publication and consumer checks remain part of release preparation. See the [sample project](sample/) for source dependencies during development.

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
- `CrossUidPassThrough.SAME_UID_ONLY` is the default. Opt into `WHEN_SYSTEM_ALLOWS` only when passing touches to another app is required. Android's opacity and window policies can reject this with `PASS_THROUGH_OPACITY_EXCEEDED` or `PASS_THROUGH_UNSUPPORTED`; the library cannot guarantee another app receives the touch.
- Geometry uses the owning window's bounds and insets. Obtain slider limits from the host window, with a compatible fallback on older supported APIs. `allowOutsideBounds` relaxes normal bounds handling; it does not guarantee placement over system bars or other restricted areas.
- `screenBrightness` is an optional Activity-scope setting. Supplying it to an application overlay is an argument error.

Factories, operations, and `view`/`spec` access belong on the main thread. Only `state` and `lastFailure` are thread-safe handle reads. `OverlayPermission` is immutable and thread-safe. Timber's logging entry point accepts background-thread calls.

## Optional Timber overlay

Initialize the manager first, then plant one tree from the Application. Register each Activity whose visible lifetime should show the log overlay.

```java
DebugOverlayTree tree = DebugOverlayTree.init(this);
tree.setThreshold(Log.DEBUG);
tree.setMaxLines(5);
Timber.plant(tree);
```

```java
// In Activity.onCreate():
DebugOverlayTree.getInstance().register(this);
```

Same-Application `init()` is idempotent, but the host should still avoid planting the same tree repeatedly. The Timber overlay is application-scoped and requires overlay permission to show; failed shows are reported through the library logger.

To stop it, call `dispose()` on the main thread and retain the tree on failure:

```java
OverlayResult result = tree.dispose();
if (result.isSuccess()) {
    Timber.uproot(tree);
} else {
    Log.w("Overlay", "Disposal failed: " + result.getFailure(), result.getCause());
    // Keep tree and retry only when the host requests it.
}
```

Successful disposal unregisters callbacks and clears the buffer and Activity caches. Later logs and old queued renders become no-ops; a later initialization starts a fresh lifecycle generation.

## Development and limits

See [the sample](sample/) and [the 3.0.0 migration guide](docs/migration/2.x-to-3.0.md). The library does not provide background execution, a foreground service, notifications, or automatic permission recovery. The host owns those responsibilities for long-running application overlays. OS and device window policies still apply.

The release's device matrix, including cross-UID pass-through and edge-to-edge positioning, is tracked in [V3_0_0_PLAN.md](V3_0_0_PLAN.md) and is not yet complete.

## License

Copyright 2017 75py. Licensed under the [Apache License, Version 2.0](LICENSE).
