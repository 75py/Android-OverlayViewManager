#!/bin/bash

set -ev

echo "before_script: $TEST_TYPE"

if [ "$TEST_TYPE" == "instrument" ]; then
    android-wait-for-emulator
    while ! adb shell getprop init.svc.bootanim; do
      echo Waiting for boot animation to end
      sleep 10
    done
    while ! adb shell getprop ro.build.version.sdk; do
      echo Waiting for ro.build.version.sdk value from device
      sleep 10
    done
    adb devices
    adb shell settings put global window_animation_scale 0 &
    adb shell settings put global transition_animation_scale 0 &
    adb shell settings put global animator_duration_scale 0 &
    adb shell input keyevent 82 &
    adb shell setprop dalvik.vm.dexopt-flags v=n,o=v
fi

exit 0
