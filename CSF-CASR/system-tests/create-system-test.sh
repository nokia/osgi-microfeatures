#!/bin/bash

if [ $# -lt 2 ]; then
    echo "Usage: $O <obr url> <test name> <test feature> OR $0 <obr url> <system-test-list>"
    echo "system-test-list is a text file containing one system feature per line"
    echo "and optionally the test name after a blank space"
    exit 1
fi


SYSTEMTESTS=`which $0`
SYSTEMTESTS=`dirname $SYSTEMTESTS`
SYSTEMTESTS=`(unset CDPATH ; cd $SYSTEMTESTS ; pwd)`


if [ "$SYSTEMTESTS" == "" ]; then
    echo "Can't determine system-tests directory."
    exit 1
fi

RUNTIME=`(unset CDPATH ; cd $SYSTEMTESTS/../runtime ; pwd)`
cd $SYSTEMTESTS

if [ "$1" == "-m2" ]; then
    obr=file://$HOME/.m2/repository/obr.xml
else
    obr=$1
fi

shift

bulk_stest_create() {
    local stestList=$1; shift
    echo "bulk instanciating system tests in $stestList"
    
    local finalArg="${obr} "
    local stestArray=()
    while IFS= read -r line
    do
        if [[ "$line" = \#* ]] || [[ -z "$line" ]] ; then
            continue
         fi

        stestArray+=($line)
        finalArg="${finalArg}$line 0.0.0 $line,"
    done < "$stestList"

    finalArg=${finalArg%?}
    $RUNTIME/create-multiple-runtimes.sh $finalArg

    echo "unpacking zips..."
    for stest in ${stestArray[@]}; do
        unzip -q /tmp/$stest-0.0.0.zip -x / -d $SYSTEMTESTS
        rm /tmp/$stest-0.0.0.zip
        mv $SYSTEMTESTS/$stest-0.0.0 $SYSTEMTESTS/$stest
        rm -rf $SYSTEMTESTS/$stest/instance
        rm -rf $SYSTEMTESTS/$stest/bundles
        rm -f $SYSTEMTESTS/$stest/start.sh
        rm -f $SYSTEMTESTS/$stest/stop.sh
        mv $SYSTEMTESTS/$stest/stest/* $SYSTEMTESTS/$stest
        rmdir $SYSTEMTESTS/$stest/stest
    done
    echo "done"
    exit 0
}

if [ -f "$1" ]; then
    bulk_stest_create "$1"
fi

name=$1
feature=$2

if [ "$name" == "" ]; then
    echo "system name can't be empty string."
    exit 1
fi

if [ -d "$name" ]; then
    echo "Removing old system test $name"
    rm -rf $name
fi

../runtime/create-runtime.sh $obr test 0.0.0 "$feature"
mkdir -p $name
cd $name
tar zxvf /tmp/runtime/test-0.0.0.tgz
mv test-0.0.0/* .
rmdir test-0.0.0
rm -rf instance
rm -f start.sh
rm -f stop.sh
mv stest/* .
rmdir stest



