#!/bin/sh
# execute-backup-command.sh

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