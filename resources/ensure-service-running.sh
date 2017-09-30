#!/bin/bash

#Ensure service is running
SERVICES=$(docker service ls --filter name=${IMAGE_NAME} --quiet | wc -l)
if [[ "$SERVICES" -eq 0]]; then
    docker service create \
        --name ${IMAGE_NAME} \
        --restart-condition any \
        --restart-delay 5s \
        --update-delay 10s \
        --update-parallelism 1 \
        ${IMAGE_NAME}
fi
