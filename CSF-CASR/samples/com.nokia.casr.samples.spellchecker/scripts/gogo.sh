#!/bin/sh

USER=`whoami`
CWD=`which $0`
CWD=`dirname $CWD`
HOST=${1:-localhost}
PORT=${2:-17001}

if [ "$HOST" == "help" ]; then
    echo "Usage: $0 [host port]"
    echo
    echo "This script allows to connect to a given running csf server using a bash-like interactive "gogo" shell"
    echo "(by default host used is localhost, and port is 17001)"
    exit 1
fi

java -jar $CWD/gogo.jar $USER $HOST $PORT
