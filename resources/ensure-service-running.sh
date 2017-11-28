#!/bin/bash

#Ensure service is running

echo "IMAGE_NAME: $1"
echo "PORT: $2"
echo "PIPELINE_VERSION: $3"
echo "CONSUL_ENVIRONMENT: $4"
echo "CONSUL_ADDRESS: $5"

IMAGE_NAME=$1
PORT=$2
PIPELINE_VERSION=$3
CONSUL_ENVIRONMENT: $4
CONSUL_ADDRESS: $5

SERVICE_NAME="petprojects/${IMAGE_NAME}";

IMAGE_NAME_WITH_VERSION="$SERVICE:${PIPELINE_VERSION}";

echo "Check service is running - $SERVICE_NAME";

SERVICES=$(docker service ls -f name=${IMAGE_NAME} --quiet | wc -l)

echo "SERVICES VALUE Should be 0. Actual Value = $SERVICES | PORT : ${PORT}";

failureCode=0
if [ $SERVICES -eq 0 ]; then

    echo "Creating Service - ${IMAGE_NAME}";    

	lastExitCode=0
	if [ -z "${PORT}" ] ; then
		docker service create \
			--name $IMAGE_NAME \
			--replicas 3 \
			--restart-condition any \
			--restart-delay 5s \
			--update-delay 10s \
			--update-parallelism 1 \
			--env-add MTS_APP_SETTINGS_ConsulStoreConfiguration:Environment=${CONSUL_ENVIRONMENT} \
			--env-add MTS_APP_SETTINGS_ConsulClientConfiguration:Address=${CONSUL_ADDRESS} \
			$SERVICE_NAME:${PIPELINE_VERSION};
		lastExitCode=$?;
	else
		docker service create \
			--name $IMAGE_NAME \
			--replicas 3 \
			--restart-condition any \
			--restart-delay 5s \
			--update-delay 10s \
			--update-parallelism 1 \
			--publish ${PORT}:80 \
			--env-add MTS_APP_SETTINGS_ConsulStoreConfiguration:Environment=${CONSUL_ENVIRONMENT} \
			--env-add MTS_APP_SETTINGS_ConsulClientConfiguration:Address=${CONSUL_ADDRESS} \
			$SERVICE_NAME:${PIPELINE_VERSION};
		lastExitCode=$?;
    fi;
	echo "lastexitcode=$lastExitCode";
	if [ $lastExitCode != 0 ] ; then
		echo "setting failurecode $lastExitCode";
		failureCode=$lastExitCode;
	else
		SERVICES_AFTER_CREATION=$(docker service ls -f name=${IMAGE_NAME} --quiet | wc -l)
		if [ $SERVICES_AFTER_CREATION -eq 1 ]; then
			echo "Service is Created - ${IMAGE_NAME}";
		else
			echo "SERVICES_AFTER_CREATION VALUE Should be 1. Actual Value = $SERVICES_AFTER_CREATION";
			echo "Service is NOT Created - ${IMAGE_NAME}";
		fi;
	fi;
fi;

if [ $failureCode != 0 ] ; then
	echo "exiting with status code $failureCode";
	exit $failureCode;
fi;
