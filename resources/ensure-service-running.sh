#!/bin/bash

#Ensure service is running

echo "Check service is running - ${IMAGE_NAME}";

SERVICES=$(docker service ls -f name=${IMAGE_NAME} --quiet | wc -l)

if [[ $(docker service ls -f name=${IMAGE_NAME} --quiet | wc -l) -eq 0 ]]; then
    docker service create \
        --name ${IMAGE_NAME} \
        --restart-condition any \
        --restart-delay 5s \
        --update-delay 10s \
        --update-parallelism 1 \
        ${IMAGE_NAME}
fi
