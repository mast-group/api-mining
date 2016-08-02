#!/bin/bash
for i in \
andengine,0.4 \
camel,0.2 \
cloud9,0.005 \
drools,0.2 \
hornetq,0.005 \
mahout,0.005 \
neo4j,0.06 \
netty,0.005 \
resteasy,0.005 \
restlet-framework-java,0.03 \
spring-data-mongodb,0.005 \
spring-data-neo4j,0.005 \
twitter4j,0.005 \
webobjects,0.12 \
weld,0.005 \
wicket,0.3 \
hadoop,0.3
do IFS=',' 
set $i
java -cp api-mining/target/api-mining-1.0.jar apimining.upminer.UPMiner \
-f datasets/calls/all/$1.arff \
-o output/all/$1/upminer/ \
-s $2
done 
