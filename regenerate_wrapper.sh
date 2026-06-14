#!/bin/sh
# This repository's gradle-wrapper.jar binary could not be included by the
# automated fix (no network access in the build/edit environment).
#
# Run this ONCE on a machine that has Gradle installed (or Android Studio,
# which bundles Gradle) to regenerate gradle/wrapper/gradle-wrapper.jar:
#
#   gradle wrapper --gradle-version 8.4
#
# After that, gradlew / gradlew.bat will work normally and you never need
# to run this again (commit the generated jar to version control).
#
# If you don't have Gradle installed, simply open this project in
# Android Studio once — it will offer to set up / regenerate the wrapper
# automatically.

set -e
if command -v gradle >/dev/null 2>&1; then
  gradle wrapper --gradle-version 8.4
  chmod +x gradlew
  echo "gradle-wrapper.jar regenerated successfully."
else
  echo "Gradle is not installed. Open this project in Android Studio instead,"
  echo "or install Gradle 8.4 and re-run this script."
  exit 1
fi
