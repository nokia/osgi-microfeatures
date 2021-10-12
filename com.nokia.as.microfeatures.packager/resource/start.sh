#!/bin/sh
# Copyright 2000-2021 Nokia
#
# Licensed under the Apache License 2.0
# SPDX-License-Identifier: Apache-2.0
#

# Auto generated wrapper script for scripts/start.sh

INSTANCE=instance

while [ $# -gt 0 ]
do
    key="$1"
    case $key in
	-h|--help)
	    echo "Usage:"
	    echo "$0 [-f|--foreground] [-l||--log] [-l||--log \"list of loggers\"] [-r|--restart]"
	    echo "$0 [-f|--foreground] [-l||--log] [-l||--log \"list of loggers\"] [-r|--restart] instance_dir"
	    echo "(by default the instance directory used is \"instance\")"
	    exit 1
	    ;;
	-f|--foreground)
	    FOREGROUND="true"
	    ;;
	-l|--log)
	    shift
	    if [ $# = 0 ]
	    then
		export CASR_LOGSTDOUT="rootLogger=WARN"		
	    else
		export CASR_LOGSTDOUT="${1}"
	    fi
	    ;;
	-t|--tail)
	    TAIL=true
	    ;;
	-r|--restart)
	    RESTART=true
	    ;;
	*)
            INSTANCE=${1}
	    ;;
    esac
    shift # past argument or value
done

INSTALL_DIR=`which $0`
INSTALL_DIR=`dirname $INSTALL_DIR`
export INSTALL_DIR=`(unset CDPATH ; cd $INSTALL_DIR ; pwd)`
cd $INSTALL_DIR

CMD_PATH=$INSTALL_DIR/scripts/start.sh

#Just tag this instance path, so we can check if it's already running from /proc/pid/environ
export INSTANCE_FULLPATH="$INSTALL_DIR/$INSTANCE"

if [ "$RESTART" = "true" ]
then
    ./stop.sh $INSTANCE
    sleep 1
fi

# security check for previously launched instance
mkdir -p $INSTALL_DIR/var/tmp/pids
PIDFILE=$INSTALL_DIR/var/tmp/pids/${INSTANCE}.pid
PID=0 
[ -e $PIDFILE ] && PID=`cat $PIDFILE`
if [ "$PID" != "" ] && [ -d /proc/$PID ]
then
  echo "$INSTANCE already started with pid $PID"
  exit 0
fi

#Get a UUID. If (rc!=0), simply exit
GETUID_CMD=$INSTALL_DIR/scripts/getUuid.sh 
if [ -f $GETUID_CMD ]
then
  $GETUID_CMD "csf/runtime/component/$INSTANCE" $INSTALL_DIR/$INSTANCE/system.cfg
  RC=$?
  [ $RC != 0 ] && exit $RC
fi

if [ "$CASR_LOGSTDOUT" != "" ] || [ "$FOREGROUND" = "true" ]
then
    $CMD_PATH -r $INSTALL_DIR -h localhost -p csf -g runtime -c component -i "$INSTANCE" -d $INSTALL_DIR/$INSTANCE
else
    # if an old scripts/start.sh script is detected, we need to launch the script in background, else we can use the "-pid" option
    # (newer scripts accept -pid option to launch the process in background)
    # TO detect if the scripts/start.sh supports the -pid option, we test the output of the "-help" option, which
    # displays "-r <root dir> -h hostname -p platformName -g group -c component -i instance -d configDir [-pid pidfile]"
    $CMD_PATH -help|grep pidfile
    if [ $? = 0 ]
    then
	$CMD_PATH -r $INSTALL_DIR -h localhost -p csf -g runtime -c component -i "$INSTANCE" -d $INSTALL_DIR/$INSTANCE -pid $PIDFILE
    else
	$CMD_PATH -r $INSTALL_DIR -h localhost -p csf -g runtime -c component -i "$INSTANCE" -d $INSTALL_DIR/$INSTANCE &
	PID=$!
	echo ${PID} > $PIDFILE
    fi

    if [ "$TAIL" = "true" ]
    then
	tail -f var/log/*${INSTANCE}/*.log
    fi
fi



