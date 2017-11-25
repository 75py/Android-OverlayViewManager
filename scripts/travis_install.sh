#!/bin/bash

set -ev

echo "install: $TEST_TYPE, $TARGET_PROJECT"

if [ "$TEST_TYPE" == "unit" ]; then
    ./gradlew :${TARGET_PROJECT}:assembleDebug --stacktrace
elif [ "$TEST_TYPE" == "instrument" ]; then
    ./gradlew :${TARGET_PROJECT}:assembleDebugAndroidTest --stacktrace
else
    exit 1
fi

exit 0
