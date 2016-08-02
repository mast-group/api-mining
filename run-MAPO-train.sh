#!/bin/bash
for i in \
andengine,0.09 \
camel,0.007 \
cloud9,0.02 \
drools,0.06 \
hornetq,0.06 \
mahout,0.3 \
neo4j,0.005 \
netty,0.005 \
resteasy,0.07 \
restlet-framework-java,0.2 \
spring-data-mongodb,0.005 \
spring-data-neo4j,0.005 \
twitter4j,0.3 \
webobjects,0.007 \
weld,0.005 \
wicket,0.2 \
hadoop,0.008
do IFS=',' 
set $i
java -cp api-mining/target/api-mining-1.0.jar apimining.mapo.MAPO \
-f datasets/calls/train/$1.arff \
-o output/train/$1/mapo/ \
-s $2
done 
