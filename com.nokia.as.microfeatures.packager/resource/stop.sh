#!/bin/sh
# Auto generated wrapper script for scripts/stop.sh
echo stop ${1-instance}

INSTALL_DIR=`which $0`
INSTALL_DIR=`dirname $INSTALL_DIR`
export INSTALL_DIR=`(unset CDPATH ; cd $INSTALL_DIR ; pwd)`
cd $INSTALL_DIR
INSTANCE=${1-instance}
CMD_PATH=$INSTALL_DIR/scripts/stop.sh

$CMD_PATH -r $INSTALL_DIR -h localhost -p csf -g runtime -c component -i "$INSTANCE" -d $INSTALL_DIR/$INSTANCE > /dev/null 2>&1
