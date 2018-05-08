#!/usr/bin/env bash

set -euo pipefail

cd integration
../gradlew clean
../gradlew -Pl10nFixVerbosity=1 assembleDebug test --stacktrace

aapt=$ANDROID_HOME/build-tools/$(ls $ANDROID_HOME/build-tools | tail -n 1)/aapt

function die() {
    echo $1
    exit 1
}

function resgrep() {
    $aapt d --values resources app/build/outputs/apk/debug/app-debug.apk resources.arsc |
        grep -F "$1"
}

! resgrep "config pt" ||
    (resgrep "config" && die "APK included test-only resources")

resgrep "type 10 configCount=7" ||
    (resgrep "config" && die "APK had wrong number of language resources")

echo "Tests complete"
