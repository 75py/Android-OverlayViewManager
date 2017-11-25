#!/bin/bash

set -ev

echo "install: $TEST_TYPE"

if [ "$TEST_TYPE" == "unit" ]; then
    ./gradlew assembleDebug --stacktrace
elif [ "$TEST_TYPE" == "instrument" ]; then
    ./gradlew assembleDebugAndroidTest --stacktrace
else
    exit 1
fi

exit 0
