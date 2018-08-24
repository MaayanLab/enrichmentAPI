# get rid of old stuff
docker rmi -f $(docker images | grep "^<none>" | awk "{print $3}")
docker rm $(docker ps -q -f status=exited)

docker kill enrichmentapi
docker rm enrichmentapi

#docker build -f DockerStar -t maayanlab/aligner-amazon .
docker build -f DockerfileAPI -t maayanlab/enrichmentapi .

#docker push maayanlab/aligner-amazon
docker push maayanlab/enrichmentapi

#docker run -d --name="jobscheduler" -p 8989:80 maayanlab/awsjobscheduler

