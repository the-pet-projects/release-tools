chmod +x run-integration-tests.sh
docker-compose -f docker-compose.integrationtests.yml -p netcoreintegrationtests run ci-integration-tests

