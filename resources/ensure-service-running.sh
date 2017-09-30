#!/bin/bash

#Ensure service is running

SERVICE_NAME="petprojects/${IMAGE_NAME}:${PIPELINE_VERSION}";

echo "Check service is running - ${IMAGE_NAME}";

SERVICES=$(docker service ls -f name=${$SERVICE_NAME} --quiet | wc -l)

echo "SERVICES VALUE Should be 0. Actual Value = $SERVICES";

if [ $SERVICES -eq 0 ]; then

    echo "Creating Service - $SERVICE_NAME";

    docker service create \
        --name $SERVICE_NAME \
        --restart-condition any \
        --restart-delay 5s \
        --update-delay 10s \
        --update-parallelism 1 \
        $CONTAINER_NAME

    echo "Service is Created - $SERVICE_NAME"
fi
