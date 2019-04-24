#!/bin/sh
# execute-backup-command.sh
set -e

echo "ENVIRONMENT=${ENVIRONMENT}"

IP_ADDRESS=$(curl -s http://checkip.amazonaws.com/)
echo ""
echo "-----------------------------------------------------------------------------------------------------------------"
echo ""
echo "Executing command: '${COMMAND} ${ADDITIONAL_COMMAND_OPTIONS}' in environment: '${ENVIRONMENT}' with the following external IP address: ${IP_ADDRESS}"
echo ""
echo "-----------------------------------------------------------------------------------------------------------------"
echo ""

cerberus -e ${ENVIRONMENT} --no-tty ${COMMAND} ${ADDITIONAL_COMMAND_OPTIONS}

curl -s -X POST \
  https://ingest.signalfx.com/v2/datapoint \
  -H 'Cache-Control: no-cache' \
  -H 'Content-Type: application/json' \
  -H "X-SF-TOKEN: ${SIGNALFX_TOKEN}" \
  -d "{ \"counter\": [{
       \"metric\": \"cerberus.cli.success\",
       \"dimensions\": { \"command\": \"${COMMAND}\", \"env\": \"${ENVIRONMENT}\" },
       \"value\": 1
  }]}"