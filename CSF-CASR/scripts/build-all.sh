#!/bin/bash

# This script can be used to build and install everything from scratch

SCRIPTDIR=`which $0`
SCRIPTDIR=`dirname $SCRIPTDIR`
SCRIPTDIR=`(unset CDPATH ; cd $SCRIPTDIR ; pwd)`

if [ "$SCRIPTDIR" == "" ]; then
    echo "Can't determine script dir."
    exit 1
fi

export GRADLE_OPTS="-Xmx4g -Xms4g"

function usage {
	echo "$0 [-baseline]"
	exit 1
}

# Check parameters
while  [ ! $# = 0 ]
do case $1 in
       -h|-help|--help)
	   usage
	   ;;
       -baseline)
	   shift
	   BASELINE="true"
	   ;;
   esac
   shift
done

function make_workspace() {
    workspace=$1
    if [ ! -d $1 ]; then
	return
    fi
    echo "Rebuilding workspace `dirname $workspace`"
    cd $workspace
    rm -rf cnf/cache
    ./gradlew clean jar --no-daemon 
    if [ $? -ne 0 ]; then
	echo "compilation failed in workspace:"
	pwd
	exit 1
    fi
    if [ "$BASELINE" == "true" ]; then
	./gradlew -Dreleaserepo=Baseline release --no-daemon
	if [ $? -ne 0 ]; then
	    echo "release failed in workspace:"
	    pwd
	    exit 1
	fi
    fi
    if [ $? -ne 0 ]; then
	echo "Could not build workspace $workspace"
	exit 1
    fi
    cd -
    echo "$workspace rebuilt"
}

function make_obr() {
    cd $SCRIPTDIR/../runtime
    ./create-obr-m2.sh
}

function make_baseline() {
    if [ "$BASELINE" == "true" ]; then
	cd $SCRIPTDIR/../..
	tar zcf /tmp/baseline.tgz CSF-CASR/core/cnf/baseline CSF-CJDIA/casr/cnf/baseline CSF-CDLB/casr/cnf/baseline
	echo "Baseline stored in /tmp/baseline.tgz"
    fi
}

rm -rf $HOME/.m2/repository/*

make_workspace $SCRIPTDIR/../core
make_workspace $SCRIPTDIR/../../CSF-CJDIA/casr
make_workspace $SCRIPTDIR/../../CSF-CDLB/casr
make_obr
make_baseline
echo "rebuilt everything."




