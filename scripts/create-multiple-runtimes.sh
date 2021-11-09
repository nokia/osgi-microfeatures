#!/bin/sh

# Determine current directory
SCRIPTDIR=`which $0`
SCRIPTDIR=`dirname $SCRIPTDIR`
SCRIPTDIR=`(unset CDPATH ; cd $SCRIPTDIR ; pwd)`

if [ "$SCRIPTDIR" == "" ]; then
    echo "Can't determine runtime directory."
    exit 1
fi

if [ $# -lt 4 ]; then
    echo "Usage: $O <obr url> <appName> <appVersion> feature1:version feature2:version,<app2Name> <app2Version> feature1:version feature2:version..."
    exit 1
fi

obrUrl=$1
shift

cd ${SCRIPTDIR}
MICROFEATURE=com.nokia.csf.microfeatures.jar
if [ ! -f $MICROFEATURE ]; then
    echo "$MICROFEATURE not found, please build it using \"./gradlew com.nokia.as.microfeatures.admin:export.launch\" command"
    exit 1
fi

GOGO_SCRIPT=$(mktemp /tmp/gogo-script.XXXXXXXX)

args="$*"
IFS=',' ;for i in $args; do
  appName=$(echo "$i" | tr -s ' ' | cut -d ' ' -f1)
  echo "echo 'generating $appName'" >> $GOGO_SCRIPT
  echo "microfeatures:create" $i >> $GOGO_SCRIPT
done

obrdir=$(mktemp -d /tmp/mf-obrXXXXXX)
trap "rm -rf $obrdir" EXIT

cd /tmp
if [ -z "$JAVA_HOME" ] ; then
    java -Dverbose -Dobr.local=$obrdir -Dshell="$(echo "source "${GOGO_SCRIPT})"  -Dobr=$obrUrl -jar ${SCRIPTDIR}/$MICROFEATURE
else
    $JAVA_HOME/bin/java -Dverbose -Dobr.local=$obrdir -Dshell="$(echo "source "${GOGO_SCRIPT})" -Dobr=$obrUrl -jar ${SCRIPTDIR}/$MICROFEATURE
fi











