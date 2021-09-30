#!/bin/bash

# This script can be used to create casr microfeature bnd descriptors.

DOC="https://confluence.app.alcatel-lucent.com/"
DESC="feature description"
REQUIRE="foo.bsn1,foo.bsn2"
SCRIPTDIR=`which $0`
SCRIPTDIR=`dirname $SCRIPTDIR`
SCRIPTDIR=`(unset CDPATH ; cd $SCRIPTDIR ; pwd)`
DIR=`realpath $SCRIPTDIR/../core/com.nokia.as.features`

function usage {
    echo "Usage:"
    echo "$0 -f feature.name [-dir dirname] [-require list-of-bundle-symbolic-names] [-doc doc-url] [-desc feature-short-description]"
    echo
    echo "OPTIONS:"
    echo "	-f name: feature name, must be lower cased, no spaces, and dots can be used to separate domain."
    echo "	-d: directory where the feature bnd file is created. By default, core/com.nokia.as.feature/ is used"
    echo "	-r list-of-bundle-symbolic-names: the list of bundle symbolic names the feature is depending on. Default value: $REQUIRE"
    echo "	-doc feature-doc-url: the feature documentation url. Default value: $DOC"
    echo "	-desc feature-short-description: The feature description. Default value: $DESC"
    exit 1
}

# Check parameters
while  [ ! $# = 0 ]
do case $1 in
       -h|-help|--help)
	   usage
	   ;;
       -d)
	   shift
	   DIR=`realpath $1`
	   ;;
       -f|-feature)
	   shift
	   FEATURE=$1
	   ;;
       -r)
	   shift
	   REQUIRE=$1
	   ;;
       --doc)
	   shift
	   DOC=$1
	   ;;
       -desc)
	   shift
	   DESC=$1
	   ;;
   esac
   shift
done

echo $REQUIRE

[ "$FEATURE" == "" ] && echo "*** Missing -f option" && usage && exit 1
[ ! -d "$DIR" ] && echo "*** directory $DIR does not exist" && usage && exit 1
OUT=$DIR/${FEATURE}.bnd
[ -f $OUT ] && echo "*** bnd descriptor already exist: $OUT" && exit 1

echo "Bundle-Name: $FEATURE CASR Feature" > $OUT
echo "Bundle-Version: 1.0.0" >> $OUT
echo "Private-Package: " >> $OUT
echo >> $OUT
echo "Provide-Capability: com.nokia.as.feature;\\" >> $OUT
echo "                    com.nokia.as.feature=\"$FEATURE\";\\" >> $OUT
echo "                    version:Version=\"1.0.0\";\\" >> $OUT
echo "                    desc=\"$DESC\";\\" >> $OUT
echo "                    doc=\"$DOC\"" >> $OUT

echo >> $OUT
echo "Require-Capability: \\" >> $OUT
require=${REQUIRE//,/$'\n'}
commas=$(echo "${REQUIRE}" | awk -F, '{print NF-1}')
for str in $require; do
    if [ "$commas" == "0" ]; then
	echo "    	osgi.identity;filter:='(osgi.identity=$str)'" >> $OUT
    else
	echo "    	osgi.identity;filter:='(osgi.identity=$str)',\\" >> $OUT
    fi
    commas=`expr $commas - 1`
done  

echo "Created $OUT descriptor."






