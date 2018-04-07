#!/usr/bin/env bash

set -euo pipefail

cd integration
../gradlew clean assembleDebug

aapt=$ANDROID_HOME/build-tools/$(ls $ANDROID_HOME/build-tools | tail -n 1)/aapt

function die() {
    echo $1
    exit 1
}

$aapt d --values resources app/build/outputs/apk/debug/app-debug.apk resources.arsc |
    grep -F "type 10 configCount=5" || die "APK had wrong number of language resources"

echo "Tests complete"
