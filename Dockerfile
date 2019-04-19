FROM openjdk:8-jre-alpine

RUN apk update
RUN apk upgrade
RUN apk add bash

COPY build/libs/cerberus.jar .
COPY cerberus-no-update.sh ./cerberus

RUN chmod +x ./cerberus
ENV PATH="/:${PATH}"
