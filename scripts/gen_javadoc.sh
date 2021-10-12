#!/bin/bash
# Copyright 2000-2021 Nokia
#
# Licensed under the Apache License 2.0
# SPDX-License-Identifier: Apache-2.0
#

onError() {
    echo "Something went wrong!"
    exit 1
}

trap onError ERR

SCRIPTDIR=`which $0`
SCRIPTDIR=`dirname $SCRIPTDIR`
SCRIPTDIR=`(unset CDPATH ; cd $SCRIPTDIR ; pwd)`

if [ "$SCRIPTDIR" == "" ]; then
    echo "Can't determine script dir."
    exit 1
fi

DEFAULT_LIST="documented_modules"
MODULE_LIST=""
if [ $# -lt 2 ]; then
    MODULE_LIST=$SCRIPTDIR/$DEFAULT_LIST
else
    MODULE_LIST=$1
fi

modules=()
cmdLine=""
while read line; do
    if [[ $line = \#* ]] || [[ -z $line ]] ; then
        continue
    fi

    cmdLine+="$line:javadoc "
    modules+=($line)
done < $MODULE_LIST

pushd $SCRIPTDIR/../core
echo "Generating javadoc..."
./gradlew $cmdLine

moduleHtmlList=""

for module in ${modules[@]}; do
    if [ -d ../javadoc/$module ] ;  then
        rm -rf ../javadoc/$module
    fi
    mkdir -p ../javadoc/$module
    mv  $module/generated/docs/javadoc/*  -t ../javadoc/$module
    moduleHtmlList+="<li> <a href=\"$module/index.html\"> $module </a> </li> \n"
done

popd
sed "s|%%MODULE_LIST%%|${moduleHtmlList}|g" $SCRIPTDIR/index.html.tpl > $SCRIPTDIR/../javadoc/index.html
ls -d $SCRIPTDIR/../

