#!/bin/bash
mvn=~/Packages/maven/bin/mvn

$mvn package -DskipTests -f pom.xml
$mvn package -DskipTests -f pom-spark.xml
