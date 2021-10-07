#!/bin/bash

# This script can be used to build and install everything from scratch

SCRIPTDIR=`which $0`
SCRIPTDIR=`dirname $SCRIPTDIR`
SCRIPTDIR=`(unset CDPATH ; cd $SCRIPTDIR ; pwd)`

cleanup() {
    echo "cleaning already installed baline"
    rm -rf CSF-CASR/core/cnf/baseline/
    rm -rf CSF-CJDIA/casr/cnf/baseline/
    rm -rf CSF-CDLB/casr/cnf/baseline/
}

if [ "$SCRIPTDIR" == "" ]; then
    echo "Can't determine script dir."
    exit 1
fi

if [ $# != 1 ]; then
    echo "$0 clean|baseline.tgz"
    echo "when specifying \"clean\" as first argument, the already installed baseline is removed"
    exit 1
fi

cd $SCRIPTDIR/../..
BASELINE=$1

if [ "$BASELINE" == "clean" ]; then
    cleanup
    exit 0
fi

if [ ! -f $BASELINE ]; then
    echo "Usage: baseline not found: $BASELINE"
    exit 1
fi

cleanup
tar zxvf $BASELINE

echo
echo "baseline installed."

