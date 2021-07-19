#!/bin/bash

mvn clean install

docker build -t gcr.io/myProj/vvp-gateway-with-pubsub:1.19 . --file vvp-gatway-dockerfile
echo push
docker push gcr.io/myProj/vvp-gateway-with-pubsub:1.19
