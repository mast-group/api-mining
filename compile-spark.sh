#!/bin/bash
mvn=~/Packages/maven/bin/mvn

$mvn install -DskipTests -f pom.xml
$mvn install -DskipTests -f pom-spark.xml
