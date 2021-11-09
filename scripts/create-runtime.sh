#!/bin/bash

# Determine current directory
SCRIPTDIR=`which $0`
SCRIPTDIR=`dirname $SCRIPTDIR`
SCRIPTDIR=`(unset CDPATH ; cd $SCRIPTDIR ; pwd)`

if [ "$SCRIPTDIR" == "" ]; then
    echo "Can't determine runtime directory."
    exit 1
fi

# Collect parameters
if [ $# -lt 4 ]; then
    echo "Usage: $O <obr url> <appName> <appVersion> feature1:version feature2:version ..."
    exit 1
fi

cd ${SCRIPTDIR}
MICROFEATURE=microfeatures.jar
if [ ! -f $MICROFEATURE ]; then
    echo "$MICROFEATURE not found, please build it using \"./gradlew com.nokia.as.microfeatures.admin:export.launch\" command"
    exit 1
fi

obrUrl=$1
shift
appName=$1
shift
appVersion=$1
shift

FEATURES=
while [[ $# -gt 0 ]]; do
    FEATURES=$1,$FEATURES
    shift
done

cd /tmp
if [ -z "$JAVA_HOME" ] ; then
    java -Dobr=$obrUrl -Dcreate=$appName,$appVersion,"$FEATURES" -jar ${SCRIPTDIR}/$MICROFEATURE
else
    $JAVA_HOME/bin/java -Dobr=$obrUrl -Dcreate=$appName,$appVersion,"$FEATURES" -jar ${SCRIPTDIR}/$MICROFEATURE
fi
 
if [ $? == 0 ]; then
    mkdir -p /tmp/runtime
    rm -rf /tmp/runtime/*
    cd /tmp/runtime/.
    unzip -q /tmp/$appName-$appVersion.zip
    rm -f /tmp/$appName-$appVersion.zip
    # create generated runtime to /tmp/runtime/ directory in the form of a tar.gz
    tar zcf /tmp/$appName-$appVersion.tgz .
    rm -rf /tmp/runtime/*
    mv /tmp/$appName-$appVersion.tgz .
    echo "Runtime created in /tmp/$appName-$appVersion.tgz"
    exit 0
fi

echo "Could not create runtime."
exit 1












