#!/bin/bash
# Copyright 2000-2021 Nokia
#
# Licensed under the Apache License 2.0
# SPDX-License-Identifier: Apache-2.0
#


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
