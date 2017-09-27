#!/bin/bash

# Update service
docker service update --image ${IMAGE_NAME}:${env.PIPELINE_VERSION} ${IMAGE_NAME}