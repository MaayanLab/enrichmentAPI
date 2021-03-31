# get rid of old stuff
docker rmi -f $(docker images | grep "^<none>" | awk "{print $3}")
docker rm $(docker ps -q -f status=exited)

docker kill enrichmentapi
docker rm enrichmentapi

docker build -f DockerfileAPI -t maayanlab/enrichmentapi:1.5.4 .

docker push maayanlab/enrichmentapi:1.5.4

