# Android OverlayViewManager [![Build Status](https://travis-ci.org/75py/Android-OverlayViewManager.svg?branch=master)](https://travis-ci.org/75py/Android-OverlayViewManager/) [![Download](https://api.bintray.com/packages/75py/maven/overlayviewmanager/images/download.svg) ](https://bintray.com/75py/maven/overlayviewmanager/) [![codecov](https://codecov.io/gh/75py/Android-OverlayViewManager/branch/master/graph/badge.svg)](https://codecov.io/gh/75py/Android-OverlayViewManager) 

OverlayViewManager provides simple APIs for displaying overlay your views.

## Usage

```java
OverlayView overlayView = OverlayView.create(yourView);

// Start overlay
overlayView.show();

// Stop overlay
overlayView.hide();
```

## Installation

Latest version: 0.1.0

```groovy
dependencies {
    compile 'com.nagopy.android:overlayviewmanager:0.1.0'
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
