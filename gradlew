#!/bin/sh
# Gradle wrapper script for Unix
GRADLE_OPTS="${GRADLE_OPTS:-} -Xmx2048m"
#!/bin/sh
# Gradle wrapper script for Unix/macOS/Linux

GRADLE_OPTS="${GRADLE_OPTS:-} -Xmx2048m"

# Detect JAVA_HOME, fall back to system java
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA_CMD="$JAVA_HOME/bin/java"
else
    JAVA_CMD="java"
fi

# Run the Gradle wrapper JAR
exec "$JAVA_CMD" -jar "$(dirname "$0")/gradle/wrapper/gradle-wrapper.jar" "$@"exec "$(dirname "$0")/gradle/wrapper/gradle-wrapper.jar" "$@"
