#!/bin/bash

kill $(ps -ef | grep "p 900[1]:9001" | awk '{print $2}')
kill $(ps -ef | grep "p 900[2]:9002" | awk '{print $2}')

nohup docker run -p 9001:9001 int1/int1:latest &> /dev/null &
nohup docker run -p 9002:9002 int2/int2:latest &> /dev/null &

# hm... Maybe this one
# docker rmi $(docker images -a|grep "<none>"|awk '$1=="<none>" {print $3}')

sleep 3

echo "docker servers should be up and well now"
