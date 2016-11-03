#!/usr/bin/env bash

# Change working directory to the location of this script
cd "$(dirname "$0")"

# Build distribution
./gradlew clean installDist

# Run
export JAVA_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,address=5005,server=y,suspend=y"
exec ./build/install/cerberus-lifecycle-cli/bin/cerberus-lifecycle-cli "$@"
