#!/bin/sh
# Gradle wrapper script for Unix
GRADLE_OPTS="${GRADLE_OPTS:-} -Xmx2048m"
exec "$(dirname "$0")/gradle/wrapper/gradle-wrapper.jar" "$@"
