#!/bin/bash

JVMOPTS="-server -Xms1g -Xmx1g"

DIR=`which $0`
DIR=`dirname $DIR`
DIR=`(unset CDPATH ; cd $DIR ; pwd)`

CP=.
LIBS=$DIR/../bundles/*.jar
for i in $LIBS; do
    CP=$CP:$i
done

java $JVMOPTS -cp $CP  com.alcatel_lucent.as.diameter.lb.test.loader.DiameterLoader "$@"
