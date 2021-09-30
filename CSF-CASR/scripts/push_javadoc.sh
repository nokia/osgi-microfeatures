#!/bin/bash
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

CASR_DOC_REPO="https://gitlabe2.ext.net.nokia.com/csf/ar/casr.git"

DEFAULT_LIST="documented_modules"
MODULE_LIST=""
if [ $# -lt 2 ]; then
    MODULE_LIST=$SCRIPTDIR/$DEFAULT_LIST
else
    MODULE_LIST=$1
fi

modules=()
while read line; do
    if [[ $line = \#* ]] || [[ -z $line ]] ; then
        continue
    fi

    modules+=($line)
done < $MODULE_LIST

DOC_DIR=$SCRIPTDIR/../javadoc

pushd `mktemp -d -t javadoc-XXXXXXXXXX`

git clone $CASR_DOC_REPO
echo "Removing existing doc"
cd casr/docs/javadoc
rm -rf *

echo "Pushing javadoc to the CASR documentation repository"

cp $DOC_DIR/index.html .
for module in ${modules[@]}; do
    cp -r $DOC_DIR/$module .
done

cd ../..
./update.sh

