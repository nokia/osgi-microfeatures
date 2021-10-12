#!/bin/bash
# Copyright 2000-2021 Nokia
#
# Licensed under the Apache License 2.0
# SPDX-License-Identifier: Apache-2.0
#


# Retrieve a unique instance UID. We first try to get an UID from the Jetty and, if it does
# not work, use a random number and cross our fingers... UIDs assigned by the Jetty are at
# most 4 digits, so we use a random number consisting of NB_DIGITS digits for differentiation

# Since get on Jetty may take time (we manage gogo shell not yet available), need to protect against already running getUuid for the same PGCI
# @exit 0: SUCCESS, 1: getUuid already running for this PGCI

# When UUID must be generated locally, number of digit to generate
NB_DIGITS=7
GENERATED_UID=""

# Get a random number consisting of the specified number of digits
# @param numDigits Number of digits
getRandom() {
    local numDigits=$1
    local res=""
    while [ ${#res} -lt $numDigits ]
    do
	res="$res$RANDOM"
    done
    GENERATED_UID=${res:0:numDigits}
    return 0
}

# Get a lock on PGCI.
# exit 1 if not able to get the lock
getLock() {
    # mkdir under linux is atomic, so no race condition here
    if ( mkdir $1 ) 2> /dev/null
    then
	return 0
    else
	echo "$1 already exist, locked by another running getUuid. Remove lock"
    fi
    # already exist, so locked by another running getUuid
    exit 1
}

releaseLock() {
    rm -rf $1
    return 0
}

# Retrieve an UID from the Jetty
# @param id Instance ID
# @param __res Where to store the result
# @return 0 if SUCCESS
getFromJetty() {
    local pgci=$1
    local M=$INSTALL_DIR/localinstances/asr/monitors/monitor/scripts/monitor.sh
    # Monitor deployed ?
    [ ! -f $M ] && return 1

    # Admin Jetty cannot use this
    echo $pgci | grep -q "asr/system/admin/"
    [ $? = 0 ] && return 1
    
    local res=$($M -doItOnJetty "asr:getuid $pgci")
    [ $? != 0 ] && return 1
    GENERATED_UID=$(echo $res | sed -e "s/_//g" | sed -e "s/ //g" | sed -e "s/\r//g" )
    return 0
}

# Retrieve a unique instance UID
# @param id Instance ID
# @param __ret Where to store the result
# @return 1 if the instance is unknown, 0 if the UID was assigned
getUid() {
    local pgci=$1
    local confFile=$2
    if [ "$confFile" == "" ]; then
	confFile=$INSTALL_DIR/localinstances/$pgci/.config/system.cfg
    fi
    [ ! -f $confFile ] && echo "$confFile not found. Cannot assign UID to $pgci" && return 1
    getFromJetty $pgci
    JETTY_RC=$?
    echo $GENERATED_UID | egrep -i -q -e "ERROR|CommandNotFoundException"
    JETTY_ERROR=$?

    # Plan B ?
    [ $JETTY_RC != 0 ] || [ $JETTY_ERROR = 0 ] && getRandom $NB_DIGITS
    
    grep -q -e "^instance.id=" $confFile
    if [ $? = 0 ]
    then
	sed -i -e "s/^instance.id=.*/instance.id=$GENERATED_UID/" $confFile
    else
	echo "instance.id=$GENERATED_UID" >> $confFile
    fi

    return 0
}

[ -z "${1:-}" ] && echo "Uasage $0 <pgci>" && exit 1

LOCK_DIR=/tmp/.lockGetUuid.$(echo $1 | sed -e 's,/,.,g')

# if a getUuid.sh is already running for this instance (rc=1), simply exit
getLock $LOCK_DIR
# first arg is pgci, second optional arg is the conf directory full path
getUid $1 $2
RC=$?
releaseLock $LOCK_DIR

exit $RC
