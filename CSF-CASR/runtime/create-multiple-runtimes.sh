#!/bin/sh

# Determine current directory
RUNTIME=`which $0`
RUNTIME=`dirname $RUNTIME`
RUNTIME=`(unset CDPATH ; cd $RUNTIME ; pwd)`
CORE=`(unset CDPATH ; cd $RUNTIME/../core ; pwd)`

if [ "$RUNTIME" == "" ]; then
    echo "Can't determine runtime directory."
    exit 1
fi

if [ $# -lt 4 ]; then
    echo "Usage: $O <obr url> <appName> <appVersion> feature1:version feature2:version,<app2Name> <app2Version> feature1:version feature2:version..."
    exit 1
fi

obrUrl=$1
shift

cd ${RUNTIME}
MICROFEATURE=com.nokia.csf.microfeatures.jar
if [ ! -f $MICROFEATURE ]; then
    wget -O $MICROFEATURE https://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered/com/nokia/casr/microfeatures/com.nokia.casr.microfeatures.main/\[RELEASE\]/com.nokia.casr.microfeatures.main-\[RELEASE\].jar
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
    java -Dverbose -Dobr.local=$obrdir -Dshell="$(echo "source "${GOGO_SCRIPT})"  -Dobr=$obrUrl -jar ${RUNTIME}/$MICROFEATURE
else
    $JAVA_HOME/bin/java -Dverbose -Dobr.local=$obrdir -Dshell="$(echo "source "${GOGO_SCRIPT})" -Dobr=$obrUrl -jar ${RUNTIME}/$MICROFEATURE
fi











