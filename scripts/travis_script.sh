#!/bin/bash

set -ev

echo "script: $TEST_TYPE, $TARGET_PROJECT"

if [ "$TEST_TYPE" == "unit" ]; then
    if [ "$TARGET_PROJECT" == "library" ]; then
        ./gradlew :${TARGET_PROJECT}:testDebug :${TARGET_PROJECT}:lintDebug :${TARGET_PROJECT}:jacocoTestDebugUnitTestReport --stacktrace
    else
        ./gradlew :${TARGET_PROJECT}:testDebug :${TARGET_PROJECT}:lintDebug --stacktrace
    fi
elif [ "$TEST_TYPE" == "instrument" ]; then
    ./gradlew :${TARGET_PROJECT}:connectedDebugAndroidTest --stacktrace
else
    exit 1
fi

exit 0
