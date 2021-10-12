#!/bin/bash
# Copyright 2000-2021 Nokia
#
# Licensed under the Apache License 2.0
# SPDX-License-Identifier: Apache-2.0
#


function set_java() {
    if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ];  then
        JAVA="$JAVA_HOME/bin/java"
    elif type -p java; then
        JAVA=java
    else
        echo "java not found in environment. either set JAVA_HOME or add java command in your PATH"
    fi
}

# parse an attribute from the gogo-bash processor
function parse_gogo_processor() {
    instance=$1
    attr=$2
    gogoserverconf=`grep gogo-bash $CWD/../$instance/defTcpServer.txt`
    echo $gogoserverconf | grep -o "$attr=[^ ]*" | sed 's/\"//g' | sed "s/$attr=//g"
}

USER=`whoami`
CWD=`which $0`
CWD=`dirname $CWD`

if [ "$HOST" == "help" ]; then
    echo "Usage: $0 [host port]"
    echo
    echo "This script allows to connect to a given running csf server using a bash-like interactive "gogo" shell"
nn    echo "(by default host used is localhost, and port is 17001)"
    exit 1
fi

set_java

GOGOCLIENT=$(find $CWD/../bundles -name *com.nokia.as.util.gogoclient*.jar|sort|tail -1)

if [ $# > 0 ] && ([ "$1" == "help" ] || [ "$1" == "-help" ] || [ "$1" == "-h" ]); then
    echo "Usage: $0 -> Will connect to the gogo specified in instance/defTcpServer.txt"
    echo "Usage: $0 host port -> Will connect to the specified gogo address"
    echo "Usage: $0 -i instance -> Will connect to the gogo specified in the provided instance"
    exit 0
elif [ $# == 2 ] && [ "$1" != "-i" ]; then
    HOST=${1}
    PORT=${2:-17001}
elif [ "$#" == "1" ]; then
    HOST=${1}
    PORT=17001
elif [ $# == 2 ] && [ "$1" == "-i" ]; then
    instance=$2
    HOST=`parse_gogo_processor $instance ip`
    PORT=`parse_gogo_processor $instance port`
elif [ $# == 0 ]; then
    HOST=`parse_gogo_processor instance ip`
    PORT=`parse_gogo_processor instance port`
fi

$JAVA -jar $GOGOCLIENT $USER $HOST $PORT
