#!/bin/bash

# Determine current directory
SCRIPTS=`which $0`
SCRIPTS=`dirname $SCRIPTS`
SCRIPTS=`(unset CDPATH ; cd $SCRIPTS ; pwd)`

if [ "$SCRIPTS" == "" ]; then
    echo "Can't determine runtime directory."
    exit 1
fi

WORKSPACE=${SCRIPTS}/..

function getLatestArtifacts() {
    for dir in $* ; do
	pushd  $dir 2>&1 > /dev/null; 
	ls -d * | while read artifact ; do 
	    pushd  "$artifact" 2>&1 > /dev/null ; 
	    ls -d * | sort -V | while read version ; do 
		if [ -f "$version/$artifact"-"$version".jar ] ; then
		    echo "$version";
		fi 
	    done | tail -n 1 | while read chosen_version ; do
		echo "$dir/$artifact/$chosen_version/$artifact-$chosen_version".jar ;
		if [ -f "$artifact/$chosen_version/$artifact-$chosen_version".jar ] ; then
		    echo "$dir/$artifact/$chosen_version/$artifact-$chosen_version".jar ;
		fi
	    done
	    popd > /dev/null ; 
	done 
	popd > /dev/null 2>&1; 
    done
}

function isBundle() {
    bsn=`unzip -q -c $1 META-INF/MANIFEST.MF|grep Bundle-SymbolicName`
    if [ "$bsn" != "" ]; then
	echo "true"
    else
	echo "false"
    fi
}

function getBundlesFromGroupId() {
    group=`echo $1|sed "s/\./\//g"`
    cd ~/.m2/repository
    if [ ! -d $group ]; then
	return
    fi
    getLatestArtifacts $group | while read i; do
	isBundle=`isBundle $i`
	if [ "$isBundle" == "true" ]; then
	    echo $i >> $GAVS
	fi
    done
    cd - > /dev/null
}

function getBundlesFromIndex() {
    index=$1
    set -- $(awk '/\# Runtime*/{f=1;next}/\[/{f=0}f{print $NF}' $index)
    for gav in `echo $@|tr " " "\n"`; do
	# Ignore commented lines
	[[ "$gav" =~ ^#.*$ ]] && continue
	colons=`echo $gav | tr -cd ':' | wc -c`
	if [ $colons -eq 2 ]; then
	    # gav is in like groupid:artifactid:version
	    g=`echo $gav|cut -f1 -d":"|sed "s/\./\//g"`
	    a=`echo $gav|cut -f2 -d":"`
	    v=`echo $gav|cut -f3 -d":"`
	    echo $g/$a/$v/$a-$v.jar >> $GAVS
	elif [ $colons -eq 4 ]; then
	    # gav is in like groupid:artifactid:type:classifier:version, like net.sf.json-lib:json-lib:jar:jdk15:2.4
	    g=`echo $gav|cut -f1 -d":"|sed "s/\./\//g"`
	    a=`echo $gav|cut -f2 -d":"`
	    type=`echo $gav|cut -f3 -d":"`
	    classifier=`echo $gav|cut -f4 -d":"`
	    v=`echo $gav|cut -f5 -d":"`
	    echo $g/$a/$v/$a-$v-$classifier.$type >> $GAVS
	else
	    echo "wrong gav found from $1: $gav"
	    exit 1
	fi
    done
}

function fixupJars() {
    cd ~/.m2/repository
    for path in `cat $GAVS`; do
	artifact=`basename $path`
	wget -O /tmp/$artifact ${releaseBaseUrl}/$path 2>/dev/null
	if [ "$?" -eq 0 ]; then
#	    echo "Reusing ${releaseBaseUrl}/$path"
	    mv -f /tmp/$artifact $path
	else
	    echo "Detected new version for $path"
	fi
    done
    cd -
}

function sortGavs() {
    cat $GAVS|sort|uniq > $GAVS2
    mv $GAVS2 $GAVS
}

function generateObr() {
    obr=$1
    obrversion=$2
    cd ~/.m2/repository
    BUNDLES=""
    for gav in `cat $GAVS`; do
	BUNDLES=`echo $BUNDLES $gav`
    done
    slash="/"
    n=$(grep -o "$slash" <<< "${obr}" | wc -l)
    ROOTPATH=""
    for((i=0;i<$n;i++)); do
	ROOTPATH="${ROOTPATH}../"
    done
    mkdir -p `dirname $obr`
    echo "Generating ~/.m2/repository/$obr and ~/.m2/repository/${obr}.gz"
    ${JAVA_HOME}/bin/java -cp $WORKSPACE/jars/repoindex.jar org.osgi.impl.bundle.bindex.cli.Index --pretty -r $obr -t ${ROOTPATH}"%p%f" -n "$obrversion" $BUNDLES
#    $SCRIPTS/../scripts/biz.aQute.bnd index -r $obr -n "$obrversion" $BUNDLES
    \cp -f $obr $obr.bak
    gzip -f $obr
    mv $obr.bak $obr
}

GAVS=`mktemp /tmp/gavs.XXXXXXXXXX`
GAVS2=`mktemp /tmp/gavs2.XXXXXXXXXX`
trap 'rm -f $GAVS $GAVS2' EXIT
obr=${1:-obr.xml}
obrversion=${2:-CASR-SNAPSHOT}
releaseBaseUrl=${3}

# get all bundles from the well known CASR/CDLB/CJDI maven group ids
echo "Getting bundles from com.nokia.casr group id"
getBundlesFromGroupId com.nokia.casr

echo "Getting bundles from com.nokia.casr.http group id"
getBundlesFromGroupId com.nokia.casr.http

echo "Getting bundles from com.nokia.casr.microfeatures group id"
getBundlesFromGroupId com.nokia.casr.microfeatures

echo "Getting bundles from com.nokia.cjdi group id"
getBundlesFromGroupId com.nokia.cjdi

echo "Getting bundles from com.nokia.cdlb group id"
getBundlesFromGroupId com.nokia.cdlb

# download jars which are already released, we want to use the same checksum when generating obrs
if [ "$releaseBaseUrl" != "" ]; then
    fixupJars
fi

# get all runtime 3rd party dependencies defined in all workspace cnf/artifactory.mvn files
echo "Collecting all 3rd party runtime dependencies"
[ -f $SCRIPTS/../cnf/central.mvn ] && getBundlesFromIndex $SCRIPTS/../cnf/central.mvn
[ -f $SCRIPTS/../cnf/confluence.mvn ] && getBundlesFromIndex $SCRIPTS/../cnf/confluence.mvn

# sort all maven coordinates
sortGavs

# finally, generate the obr in ~/.m2/repository/obr.xml
generateObr $obr $obrversion

