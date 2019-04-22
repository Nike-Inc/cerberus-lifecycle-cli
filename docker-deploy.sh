#!/usr/bin/env bash

./gradlew clean sJ

VERSION=$(cat gradle.properties | grep version | cut -d'=' -f2)

echo "Version: ${VERSION}"

IMAGE_NAME=cerberusoss/cerberus-lifecycle-management-cli

docker build -t ${IMAGE_NAME}:latest -t ${IMAGE_NAME}:${VERSION} .
docker push ${IMAGE_NAME}
