#!/bin/bash
export JAVA_HOME=/usr/lib/jvm/java-1.8.0/
export PATH=/usr/lib/jvm/java-1.8.0/bin/:$PATH
java -cp target/driver-itemset-mining-1.1-SNAPSHOT.jar itemsetmining.eval.BackgroundPrecisionRecall
