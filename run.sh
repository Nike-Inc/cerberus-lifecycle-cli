#!/usr/bin/env bash

# Change working directory to the location of this script
cd "$(dirname "$0")"

# Build distribution
./gradlew clean installDist

# Run
exec ./build/install/cerberus-lifecycle-cli/bin/cerberus-lifecycle-cli "$@"
