#!/bin/sh
# Gradle wrapper script for Unix/macOS/Linux

GRADLE_OPTS="${GRADLE_OPTS:-} -Xmx2048m"

# Detect JAVA_HOME
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA_CMD="$JAVA_HOME/bin/java"
else
    JAVA_CMD="java"
fi

# Run the wrapper JAR
exec "$JAVA_CMD" -jar "$(dirname "$0")/gradle/wrapper/gradle-wrapper.jar" "$@"
