echo "consuladdr=$CONSUL_ADDRESS"
docker-compose -f docker-compose.integrationtests.yml -p netcoreintegrationtests run ci-integration-tests

