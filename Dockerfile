FROM adoptopenjdk/openjdk11:x86_64-alpine-jre-11.0.16_8

RUN apk update
RUN apk upgrade
RUN apk add bash curl

# Copy the cerberus fat jar
COPY ./build/libs/cerberus.jar .

# Copy over our wrapper scripts
## Simple java jar wrapper to make the cli avaible as cerberus.
COPY cerberus-no-update.sh ./cerberus
## Wrapper that will execute a command from env vars, see script for details.
COPY execute-cerberus-lifecycle-cli-command.sh .
## Wrapper that will execute a command and send a success metric to sfx, see script for details.
COPY execute-cerberus-lifecycle-cli-command-signalfx.sh .

RUN chmod +x ./cerberus
ENV PATH="/:${PATH}"
