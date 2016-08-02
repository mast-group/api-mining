#!/bin/bash
for i in \
andengine,0.2 \
camel,0.005 \
cloud9,0.06 \
drools,0.2 \
hornetq,0.34 \
mahout,0.02 \
neo4j,0.05 \
netty,0.2 \
resteasy,0.1 \
restlet-framework-java,0.3 \
spring-data-mongodb,0.005 \
spring-data-neo4j,0.005 \
twitter4j,0.2 \
webobjects,0.006 \
weld,0.005 \
wicket,0.13 \
hadoop,0.4
do IFS=',' 
set $i
java -cp api-mining/target/api-mining-1.0.jar apimining.mapo.MAPO \
-f datasets/calls/all/$1.arff \
-o output/all/$1/mapo/ \
-s $2
done 
