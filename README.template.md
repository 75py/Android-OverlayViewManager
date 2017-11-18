# Android OverlayViewManager [![Build Status](https://travis-ci.org/75py/Android-OverlayViewManager.svg?branch=master)](https://travis-ci.org/75py/Android-OverlayViewManager/) [![Download](https://api.bintray.com/packages/75py/maven/overlayviewmanager/images/download.svg) ](https://bintray.com/75py/maven/overlayviewmanager/) [![codecov](https://codecov.io/gh/75py/Android-OverlayViewManager/branch/master/graph/badge.svg)](https://codecov.io/gh/75py/Android-OverlayViewManager) 

OverlayViewManager provides simple APIs for displaying overlay your views.


## Usage

### Check permissions and request if needed

```java
if (!OverlayViewManager.canDrawOverlays()) {
    OverlayViewManager.showPermissionRequestDialog(getSupportFragmentManager(), R.string.app_name);
}
```

### Create OverlayView instance

```java
OverlayView overlayView = OverlayView.create(yourView);
```

### Start and stop overlay

```java
// Start overlay
overlayView.show();

// Stop overlay
overlayView.hide();
```

<img src="images/anime/show_hide.gif" width="270" height="480" alt="">


### OverlayView#setTouchable(boolean)

```java
OverlayView overlayView = OverlayView.create(yourView)
    // .setTouchable(false) default
    .show();

overlayView.setTouchable(true)
    .update();
```

<img src="images/anime/setTouchable.gif" width="270" height="480" alt="">


### OverlayView#setDraggable(boolean)

```java
OverlayView overlayView = OverlayView.create(yourView)
    // .setDraggable(false) default
    .show();

overlayView.setDraggable(true)
    .update();
```

<img src="images/anime/setDraggable.gif" width="270" height="480" alt="">


### OverlayView#setWidth(boolean)

```java
OverlayView overlayView = OverlayView.create(yourView)
    // .setWidth(WRAP_CONTENT) default
    .show();

overlayView.setWidth(MATCH_PARENT)
    .update();

overlayView.setWidth(400)
    .update();
```

<img src="images/anime/setWidth.gif" width="270" height="480" alt="">


## Installation

### Download

Latest version: %%version%%

```groovy
dependencies {
    compile 'com.nagopy.android:overlayviewmanager:%%version%%'
}
```

### Setup

Call OverlayViewManager.init() in the onCreate() of your Application class.
```java
public class YourApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        OverlayViewManager.init(this);
    }

}
```


## License

```
Copyright 2017 75py

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
