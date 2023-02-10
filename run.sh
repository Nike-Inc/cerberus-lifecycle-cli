#!/usr/bin/env bash

# Change working directory to the location of this script
cd "$(dirname "$0")"

# Build distribution
./gradlew clean sJ

# Rename jar for convenience
mv build/libs/cerberus*.jar build/libs/cerberus.jar

# Run
java -jar ./build/libs/cerberus.jar "$@"
