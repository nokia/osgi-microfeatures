#!/bin/bash
# Copyright 2000-2021 Nokia
#
# Licensed under the Apache License 2.0
# SPDX-License-Identifier: Apache-2.0
#


#
# hashmap filled by the parse_gav function. Key=artifactid-version,  value=groupid:artifactid:version

#
# This script regroup common shell function used by other script from this directory
#

#
# Parse gav.txt file
# argument1: gav.txt file
# Parsed gavs are stored in PARSED_GAVS, which is a map of key/values:
#	Key = artifactid-classifier-version
#	Value=groupid:artitactid:type:classifier:version
function parse_gav() {
    gavfile=$1
    declare -g -A PARSED_GAVS
    
    if [ ! -f $gavfile ]; then
	echo "Missing $gavfile file. You must create a gav.txt file using mvn "
	usage
    fi
    
    for gav in `cat $gavfile | egrep '.*:.*:.*:.*:.*|.*:.*:.*'`; do
	# detect if gavs are in the form of artifact:group:type:version:scope

	TYPE=
	CLASSIFIER=
	colons=`echo $gav | tr -cd ':' | wc -c`
	if [ $colons -eq 4 ]; then
	    # gav is in like wsdl4j:wsdl4j:jar:1.6.2:compile
	    GROUP=`echo $gav|cut -f1 -d":"`
	    ARTIFACT=`echo $gav|cut -f2 -d":"`
	    VERSION=`echo $gav|cut -f4 -d":"`
	elif [ $colons -eq 2 ]; then
	    # gav is in like wsdl4j:wsdl4j:1.6.2
	    echo $gav
	    GROUP=`echo $gav|cut -f1 -d":"`
	    ARTIFACT=`echo $gav|cut -f2 -d":"`
	    VERSION=`echo $gav|cut -f3 -d":"`
	elif [ $colons -eq 5 ]; then
	    # gav is in like groupid:artifactid:type:classifier:version:scope, like net.sf.json-lib:json-lib:jar:jdk15:2.4:compile
	    GROUP=`echo $gav|cut -f1 -d":"`
	    ARTIFACT=`echo $gav|cut -f2 -d":"`
	    TYPE=`echo $gav|cut -f3 -d":"`
	    CLASSIFIER=`echo $gav|cut -f4 -d":"`
	    VERSION=`echo $gav|cut -f5 -d":"`
	    VERSION=$VERSION
	else
	    echo "wrong gavs format found from $gavfile; Expecting format like groupid:artitactid:version or groupid:artifactid:packaging:version:scope"
	    exit 1
	fi

	PARSED_GAVS[$ARTIFACT-$VERSION]="$GROUP:$ARTIFACT:$TYPE:$CLASSIFIER:$VERSION"
	PARSED_GAVS[$ARTIFACT-$VERSION-$CLASSIFIER]="$GROUP:$ARTIFACT:$TYPE:$CLASSIFIER:$VERSION"
    done
}

#
# function called when the gav is not found from gav.txt; the gav is derived from the jar's pom.xml
# input: jarfile
# scope: private
function parse_gav_from_pom() {
    local jarfile=$1
    jarfile=`realpath $jarfile`

    unzip -l $jarfile |grep "META-INF/maven/.*/pom.xml" > /dev/null 2>&1
    if [ $? -ne 0 ]; then
	# no pom found
	return
    fi

    # obtain artifact name (that is: get "artifact" from "artifact-x.y.z.jar")
    version="${jarfile##*-}"
    version="${version%.*}"
    artifact=`echo $jarfile|sed 's/-[0-9]\+.*//'`
    artifact=`basename $artifact`
    tmpdir=$(mktemp -d)
    trap "rm -rf $tmpdir" EXIT
    cd $tmpdir
    jar xf $jarfile META-INF/maven
    poms=`find . -name pom.properties`
    pomsCount=`find . -name pom.properties|wc -l`
    if [ "$poms" != "" ]; then
        for pom in $poms; do
            pomdir=`dirname $pom`
            pomdir=`basename $pomdir`
            if [[ $artifact == $pomdir ]] || [ $pomsCount == 1 ] ; then
                g=`cat $pom| grep groupId | sed "s/groupId=\([.]*\)/\1/"`
                g=`echo $g|tr -d '[:space:]'`
                a=`cat $pom| grep artifactId | sed "s/artifactId=\([.]*\)/\1/"`
                a=`echo $a|tr -d '[:space:]'`
                v=`cat $pom| grep version | sed "s/version=\([.]*\)/\1/"`
                v=`echo $v|tr -d '[:space:]'`
		# for now, we don't parse classifier
		classifier=
		type=
		classifier=
		PARSED_GAVS[$a-$v]="$g:$a:$type:$classifier:$v"
		PARSED_GAVS[$a-$v-$classifier]="$g:$a:$type:$classifier:$v"
                cd - > /dev/null
                return
            fi
        done
    fi
    cd - > /dev/null
}

#
# Return the gav for a given jar file
# argument1: a jar file, which can have the form of artifactid-version.jar or artifactid-version-classifier.jar
# output the gav in the form of groupid:artitactid:type:classifier:version
#
function get_gav() {
    local jarfile=$(basename $1 .jar)    
    gav=$(echo ${PARSED_GAVS[$jarfile]})
    if [ "$gav" == "" ]; then
	# gav not found from gav.txt for this jar, try to derive it from the jar's pom.xml (if any is found)
	parse_gav_from_pom $1
	gav=$(echo ${PARSED_GAVS[$jarfile]})
    fi
    echo "$gav"
}

#
# parse a jar's manifest header
# Input: jarfile header
# output: header value or empty string
#
function parse_manifest_header() {
    jarfile=$1
    header=$2

    # Following inspired from https://codereview.stackexchange.com/questions/74087/extracting-the-classpath-entries-from-a-jar-files-manifest
    unzip -q -c "$jarfile" META-INF/MANIFEST.MF |
		# remove carriage return
		sed 's/\r$//g' |
		# join multi-line headers in one single line
		awk '/^[^ ]/ { print BUF; BUF=$0 } END     { print BUF } /^ /    { sub(" *", ""); BUF = BUF $0 }' |
		# only display bsn
		grep "^${header}: " |
		# remove header name
		sed 's/^.*: //' |
		# remove trailing space
		xargs
}

