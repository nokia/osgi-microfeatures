#!/bin/sh

for jarfile in `find ../casr/cnf/release/ -name *.jar`; do
    echo "Extract pom from $jarfile"
    jar xf $jarfile META-INF/maven
    pom=`find META-INF/ -name pom.xml`
    prop=`find META-INF/ -name pom.properties`
    g=`cat $prop| grep groupId | sed "s/groupId=\([.]*\)/\1/"`
    g=`echo $g|tr -d '[:space:]'`
    a=`cat $prop| grep artifactId | sed "s/artifactId=\([.]*\)/\1/"`
    a=`echo $a|tr -d '[:space:]'`
    v=`cat $prop| grep version | sed "s/version=\([.]*\)/\1/"`
    v=`echo $v|tr -d '[:space:]'`
    if [ "$v" != "*-SNAPSHOT" ]; then
        cp $pom ${a}-${v}.pom
        cp $jarfile ${a}-${v}.jar
    fi
    rm -rf META-INF
done
