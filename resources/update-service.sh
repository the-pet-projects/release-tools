#!/bin/bash

SERVICE_IMAGE_TAG="petprojects/${IMAGE_NAME}:${PIPELINE_VERSION}";

SERVICES=$(docker service ls -f name=${IMAGE_NAME} --quiet | wc -l)

echo "SERVICES VALUE Should be 1. Actual Value = $SERVICES";

# Update service
if [ $SERVICES -eq 1 ]; then
    docker service update \
        --image $SERVICE_IMAGE_TAG \
        --restart-condition any \
        --restart-delay 5s \
        --update-delay 10s \
        --update-parallelism 1 \
        --publish ${PORT}:80 \
        ${IMAGE_NAME}
else
    echo "Service is NOT Created - ${IMAGE_NAME} thus WILL NOT BE UPDATED";
fi