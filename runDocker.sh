#!/bin/bash

docker container stop $(docker container ps | grep "java -jar /app1/in" | awk '{print $1}')
docker container stop $(docker container ps | grep "java -jar /app2/in" | awk '{print $1}')


nohup docker run --network host -p 9001:9001 int1/int1:latest &> /dev/null &
nohup docker run --network host -p 9002:9002 int2/int2:latest &> /dev/null &

# hm... Maybe this one
# docker rmi $(docker images -a|grep "<none>"|awk '$1=="<none>" {print $3}')

sleep 3

echo "docker servers should be up and well now"
