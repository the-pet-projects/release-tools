docker-compose -f docker-compose.integrationtests.yml -p netcoreintegrationtests rm -fsv
docker-compose -f docker-compose.release.yml rm -fsv
docker image rm -f petprojects/${IMAGE_NAME}:${BUILD_VERSION}
