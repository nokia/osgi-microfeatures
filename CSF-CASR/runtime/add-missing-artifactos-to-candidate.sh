#!/bin/bash

# This script copies some missing artifactory from csf candidate repositories.
# We scan all artifacts from cnf/artifactory.mvn , under the "Runtime "section.
# For each one; we check if the group id starts with com.nokia.
# Then each nokia artifacts are the downloaded from the csf-mvn-delivered repo
# to the given csf candidate repo.

# Determine current directory
RUNTIME=`which $0`
RUNTIME=`dirname $RUNTIME`
RUNTIME=`(unset CDPATH ; cd $RUNTIME ; pwd)`

if [ "$RUNTIME" == "" ]; then
    echo "Can't determine runtime directory."
    exit 1
fi


CANDIDATE=https://repo.lab.pl.alcatel-lucent.com/csf-mvn-snapshots-local
USER=admin
PASSWORD=password

function usage() {
    echo "$0 [-u <user>] [-p <password>] [-c <candidate repo>]"
    exit 1
}

while  [ ! $# = 0 ]
do case $1 in
       -h|-help|--help)
	   usage
	   ;;
       -u)
	   shift
	   USER=$1
	   ;;
       -p)
	   shift
	   PASSWORD=$1
	   ;;
       -c)
	   shift
	   CANDIDATE=$1
	   ;;
   esac
   shift
done


function get_bundles_from_index() {
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
	    echo $g/$a/$v/$a-$v.jar
	elif [ $colons -eq 4 ]; then
	    # gav is in like groupid:artifactid:type:classifier:version, like net.sf.json-lib:json-lib:jar:jdk15:2.4
	    g=`echo $gav|cut -f1 -d":"|sed "s/\./\//g"`
	    a=`echo $gav|cut -f2 -d":"`
	    type=`echo $gav|cut -f3 -d":"`
	    classifier=`echo $gav|cut -f4 -d":"`
	    v=`echo $gav|cut -f5 -d":"`
	    echo $g/$a/$v/$a-$v-$classifier.$type
	fi
    done
}

get_bundles_from_index $RUNTIME/../core/cnf/artifactory.mvn | while read path; do
    if [[ $path == com\/nokia\/* ]]; then
	# copy the artifactor from delivered-local repo if it's not present in csf candidate
	echo "Checking $CANDIDATE/$path"
	status=`curl -I $CANDIDATE/$path 2>/dev/null  | head -n 1 | cut -d' ' -f2`
	if [ $status -ne 200 ]; then
	    status=`curl -I https://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/${path} 2>/dev/null | head -n 1 | cut -d' ' -f2`
	    echo $status https://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/${path}
	    if [ $status -eq 200 ]; then
		echo "Copying https://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/${path} to ${CANDIDATE}/${path}"
		curl -o /tmp/jar.jar https://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/${path}
		if [ $? -ne 0 ]; then
		    echo "could not download  https://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/${path}"
		    exit 1
		fi
		curl -u $USER:$PASSWORD -X PUT ${CANDIDATE}/${path} -T /tmp/jar.jar
		if [ $? -ne 0 ]; then
		    echo "could not upload  https://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/${path} to ${CANDIDATE}/${path}"
		    exit 1
		fi
	    fi
	fi
    fi
done
