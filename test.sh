#!/usr/bin/env bash

set -euo pipefail

. gradle.properties

cd integration
../gradlew clean -PPLUGIN_VERSION=$PLUGIN_VERSION assembleDebug test

aapt=$ANDROID_HOME/build-tools/$(ls $ANDROID_HOME/build-tools | tail -n 1)/aapt

function die() {
    echo $1
    exit 1
}

$aapt d --values resources app/build/outputs/apk/debug/app-debug.apk resources.arsc |
    grep -F "type 10 configCount=6" || die "APK had wrong number of language resources"

echo "Tests complete"
