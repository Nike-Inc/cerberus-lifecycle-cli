#!/usr/bin/env bash

set +x
set +v
set -e
set -o pipefail

export CERBERUS_DOCKER_USER
export CERBERUS_DOCKER_PASSWORD

VERSION=$(cat gradle.properties | grep version | cut -d'=' -f2)

echo "Version: ${VERSION}"

IMAGE_NAME=cerberusoss/cerberus-lifecycle-management-cli

echo "${CERBERUS_DOCKER_PASSWORD}" | docker login -u ${CERBERUS_DOCKER_USER} --password-stdin
docker build -t ${IMAGE_NAME}:latest -t ${IMAGE_NAME}:${VERSION} .
docker push ${IMAGE_NAME}

docker logout
unset CERBERUS_DOCKER_PASSWORD
unset CERBERUS_DOCKER_USER
