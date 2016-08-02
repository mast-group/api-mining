#!/bin/bash
for p in \
andengine \
camel \
cloud9 \
drools \
hadoop \
hornetq \
mahout \
neo4j \
netty \
resteasy \
restlet-framework-java \
spring-data-mongodb \
spring-data-neo4j \
twitter4j \
webobjects \
weld \
wicket
do
java -cp api-mining/target/api-mining-1.0.jar apimining.pam.main.PAM -f datasets/calls/all/$p.arff -i 10000 -o output/all/$p/PAM_seqs.txt
done
