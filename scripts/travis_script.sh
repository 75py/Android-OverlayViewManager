#!/bin/bash

set -ev

echo "script: $TEST_TYPE"

if [ "$TEST_TYPE" == "unit" ]; then
    ./gradlew testDebug lintDebug jacocoTestDebugUnitTestReport --stacktrace --info
elif [ "$TEST_TYPE" == "instrument" ]; then
    ./gradlew connectedDebugAndroidTest --stacktrace --info
else
    exit 1
fi

exit 0
