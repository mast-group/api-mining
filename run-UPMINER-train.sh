#!/bin/bash
for i in \
andengine,0.05 \
camel,0.4 \
cloud9,0.01 \
drools,0.1 \
hornetq,0.01 \
mahout,0.01 \
neo4j,0.1 \
netty,0.005 \
resteasy,0.3 \
restlet-framework-java,0.01 \
spring-data-mongodb,0.005 \
spring-data-neo4j,0.005 \
twitter4j,0.005 \
webobjects,0.072 \
weld,0.005 \
wicket,0.38 \
hadoop,0.1
do IFS=',' 
set $i
java -cp api-mining/target/api-mining-1.0.jar apimining.upminer.UPMiner \
-f datasets/calls/train/$1.arff \
-o output/train/$1/upminer/ \
-s $2
done 
