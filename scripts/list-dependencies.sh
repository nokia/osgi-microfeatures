#!/bin/bash
# Copyright 2000-2021 Nokia
#
# Licensed under the Apache License 2.0
# SPDX-License-Identifier: Apache-2.0
#


# This script displays dependencies to add to CASR project
# by default, the command displays all dependencies to be added to cnf/artifactory.mvn
# the -buildpath option displays dependencies to be added to the thirdparty bnd.bnd file
# and the -feature option displays all require-capability headers to be aded to a microfeature

usage() {
    echo "Usage: $0 [-buildpath] [-features] [-g gav.txt]"
    echo "-bnd: displays declarations to be added in the CASR thirdparty's bnd.bnd file"
    echo "-feature: display all dependencies which can be added in a CASR microfeature"
    echo "-g gav.txt: the gav.txt file where all maven coordinates have been generated using dependency:resolve mvn commmand."
    echo "            (for example: mvn dependency:resolve -DincludeScope=runtime -Dsort -DoutputFile=gav.txt)"
    echo "            By default, gav.txt file is used from current directory"
    exit 1
}

function scan_jar_for_gavs() {
    jarfile=$1

    # Only display gav for jars which have been converted using a bnd file, and whose gav is found from gav.txt

    bndfile=`basename $jarfile .jar`.bnd
    if [ -f $bndfile ]; then
		gav=`get_gav $jarfile`
		if [ "$gav" != "" ]; then
		    # This jar has been converted using a bnd file, so it must be displayed in actifactory.mvn "Build time dependencies" part
		    size=${#buildtime_gavs[@]}
		    buildtime_gavs[$size]=$gav
		fi
    else
		gav=`get_gav $jarfile`
		if [ "$gav" != "" ]; then
		    # This bundle has not been converted using a bnd file, it must be displayed in artifactory.mvn "Runtime dependencies" part
		    size=${#runtime_gavs[@]}	    
		    runtime_gavs[$size]=$gav
		fi
    fi
}

# format a gav to be included in maven index file (like artifactory.mvn)
# when the gav has a classifier, we display it like groupid:artifactid:type:classifier:version
# else we display it like groupid:artifactid:version
function format_gav_for_mvnindex() {
    gav=$1
    groupid=`echo $gav|cut -f1 -d":"`
    artifactid=`echo $gav|cut -f2 -d":"`
    type=`echo $gav|cut -f3 -d":"`
    classifier=`echo $gav|cut -f4 -d":"`
    version=`echo $gav|cut -f5 -d":"`
    if [ "$classifier" == "" ]; then
	echo "$groupid:$artifactid:$version"
    else
	echo "$groupid:$artifactid:$type:$classifier:$version"
    fi
}

function display_gavs() {
    echo
    echo "# Build time dependencies (These dependencies won't be available from OBR)"
    for gav in "${buildtime_gavs[@]}"; do
	format_gav_for_mvnindex $gav
    done

    echo
    echo "# Runtime Dependencies (These dependencies will be available from OBR (do not change this comment)"
    for gav in "${runtime_gavs[@]}"; do
	format_gav_for_mvnindex $gav	
    done
    echo
}

function scan_jar_for_bnd() {
    jarfile=$1

    # Display gav for all jars whose gav is found from gav.txt

    bndfile=`basename $jarfile .jar`.bnd
    gav=`get_gav $jarfile`
    if [ "$gav" != "" ]; then
	groupid=`echo $gav|cut -f1 -d':'`
	artifactid=`echo $gav|cut -f2 -d':'`
	type=`echo $gav|cut -f3 -d':'`
	classifier=`echo $gav|cut -f4 -d':'`
	version=`echo $gav|cut -f5 -d':'`
	if [ "$classifier" == "" ]; then
	    build_dependency="$groupid:$artifactid;version=$version"
	else
	    build_dependency="$groupid:$artifactid:$type:$classifier;version=$version"
	fi
	size=${#buildpath_gavs[@]}	    
	buildpath_gavs[$size]=$build_dependency
    fi
}

function display_bnd() {
    echo "CSF-Artifact: false"
    echo "-sub: *.bnd"
    echo "-fixupmessages: \\"
    echo "   \"Can't find super class\"; is:=ignore"
    echo
    echo "-buildpath:\\"
    size=${#buildpath_gavs[@]}
    index=1
    for gav in "${buildpath_gavs[@]}"; do
	echo -n "    $gav"
	if [ "$index" -lt "$size" ]; then
	    echo ",\\"
	else
	    echo
	fi
	index=`expr $index + 1`
    done
}

function scan_jar_for_microfeatures() {
    jarfile=$1

    # Display gav for all jars whose gav is found from gav.txt

    gav=`get_gav $jarfile`
    if [ "$gav" != "" ]; then
	bsn=`parse_manifest_header $jarfile "Bundle-SymbolicName"`
	reqcap="osgi.identity;filter:='(osgi.identity=$bsn)'"
	size=${#microfeatures_reqcap[@]}	    
	microfeatures_reqcap[$size]=$reqcap
    fi
}

function display_microfeatures_reqcap() {
    echo "Require-Capability: \\"
    size=${#microfeatures_reqcap[@]}
    index=1
    for reqcap in "${microfeatures_reqcap[@]}"; do
	echo -n "    $reqcap"
	if [ "$index" -lt "$size" ]; then
	    echo ",\\"
	else
	    echo
	fi
	index=`expr $index + 1`
    done
}

function scan_jars() {
    if [ "$BND" == "true" ]; then
	for jarfile in *.jar; do
	    scan_jar_for_bnd $jarfile
	done
	display_bnd
    elif [ "$FEATURE" == "true" ]; then
	for jarfile in *.jar; do
	    scan_jar_for_microfeatures $jarfile
	done
	display_microfeatures_reqcap
    else
	for jarfile in *.jar; do
	    scan_jar_for_gavs $jarfile
	done
	display_gavs
    fi
}

# load common shell functions
function load_lib() {
    SCRIPTDIR=`which $0`
    SCRIPTDIR=`dirname $SCRIPTDIR`
    SCRIPTDIR=`(unset CDPATH ; cd $SCRIPTDIR ; pwd)`
    . $SCRIPTDIR/lib.sh
}

# ----------------------------------------------------------------------------------------------------------------

# Flag to test if microfeature dependencies must be displayed
FEATURE=false

# name of txt file containing all maven dependencies for the library being bundelizer.
# You can obtain this file using the following command:
#	mvn dependency:resolve -DincludeScope=runtime -Dsort -DoutputFile=casr/custo/gav.txt
GAVFILE=gav.txt

# array of gav to be inserted in "Build time dependencies" part, in cnf/artifactory.mvn
declare -a buildtime_gavs

# array of gav to be inserted in "Runtime dependencies" part, in cnf/artifactory.mvn
declare -a runtime_gavs

# array of gav to be inserted in CASR bnd.bnd buildpath dependencies
declare -a buildpath_gavs

# array of microfeatures require-capabilities
declare -a microfeatures_reqcap

# parse options
while  [ ! $# = 0 ]
do case $1 in
       -h|-help|--help)
	   usage
	   ;;
       -d)
	   shift
	   DIR=$1
	   # trim trailing /
	   DIR=$(echo $DIR | sed 's:/*$::')
	   ;;
       -bnd)
	   BND=true
	   ;;
       -feature)
	   FEATURE=true
	   ;;
       -g)
	   shift
	   GAVFILE=$1
	   ;;
       *)
	   usage
   esac
   shift
done

load_lib
parse_gav $GAVFILE
scan_jars

