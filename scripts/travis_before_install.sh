#!/bin/bash

set -ev

echo "before_install: $TEST_TYPE, $TARGET_PROJECT"

if [ "$TEST_TYPE" == "instrument" ]; then
    mkdir "$ANDROID_HOME/licenses" || true
    echo "8933bad161af4178b1185d1a37fbf41ea5269c55" > "$ANDROID_HOME/licenses/android-sdk-license"
    echo yes | sdkmanager tools
    echo yes | sdkmanager "system-images;android-24;default;armeabi-v7a"
    echo no | avdmanager create avd --force -n test -k "system-images;android-24;default;armeabi-v7a"
    $ANDROID_HOME/emulator/emulator -avd test -no-audio -no-window &
fi

exit 0
