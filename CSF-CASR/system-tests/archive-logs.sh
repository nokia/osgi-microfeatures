#!/bin/bash

onError() {
    echo "Something went wrong! Test aborted"
    exit 1
}

putsandbox () { 
    if [ $# -ne 1 ]; then
        echo "usage: putsandbox <filename-path>";
    else
        myname="$@";
        curl -X PUT https://repo.lab.pl.alcatel-lucent.com/sandbox-generic-inprogress-local/$(basename "$myname") -T $myname;
    fi
}

trap onError ERR

SYSTEMTESTS=`which $0`
SYSTEMTESTS=`dirname $SYSTEMTESTS`
SYSTEMTESTS=`(unset CDPATH ; cd $SYSTEMTESTS ; pwd)`
date=$(date '+%Y-%m-%d_%H:%M:%S')

tar zcvf $SYSTEMTESTS/CASR_Logs_$date.tgz $SYSTEMTESTS/stest.*/logs
putsandbox $SYSTEMTESTS/CASR_Logs_$date.tgz