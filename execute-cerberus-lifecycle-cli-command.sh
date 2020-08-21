#!/bin/sh
# execute-backup-command.sh

echo "ENVIRONMENT=${ENVIRONMENT}"
echo "REGION=${REGION}"

if [ -z ${REGION} ]; then
  REGION="us-west-2"
fi

IP_ADDRESS=$(curl -s http://checkip.amazonaws.com/)
echo ""
echo "-----------------------------------------------------------------------------------------------------------------"
echo ""
echo "Executing command: '${COMMAND} ${ADDITIONAL_COMMAND_OPTIONS}' in environment: '${ENVIRONMENT}' and region '${REGION}
 with the following external IP address: ${IP_ADDRESS}"
echo ""
echo "-----------------------------------------------------------------------------------------------------------------"
echo ""

cerberus -e ${ENVIRONMENT} -r ${REGION} --no-tty ${COMMAND} ${ADDITIONAL_COMMAND_OPTIONS}