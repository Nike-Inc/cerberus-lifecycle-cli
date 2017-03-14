#!/usr/bin/env bash

# Change working directory to the location of this script
cd "$(dirname "$0")"

# Build distribution
./gradlew clean sJ

# Run
java -Xdebug -Xrunjdwp:transport=dt_socket,address=5005,server=y,suspend=y -jar ./build/libs/cerberus.jar "$@"