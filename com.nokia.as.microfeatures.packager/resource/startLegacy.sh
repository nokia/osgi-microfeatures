#!/bin/sh
# Auto generated wrapper script for scripts/start.sh
echo start _PF_/_GRP_/_COMP_/$1

PGC=_PF_/_GRP_/_COMP_
PGCI=$PGC/$1
PGC_PATH=$INSTALL_DIR/localinstances/$PGC
PGCI_PATH=$INSTALL_DIR/localinstances/$PGCI
CMD_PATH=$PGC_PATH/scripts/start.sh

# security check for previously launched instance
PIDFILE=$PGCI_PATH/.pid
pid=0 
[ -e $PIDFILE ] && pid=`cat $PIDFILE`
if [ -d /proc/$pid ]; then
  grep -a "_ASR_INSTANCE_FULL_PATH=$PGCI" /proc/$pid/environ 2>&1 >/dev/null
  if [ "$?" = "0" ]; then
    echo "$PGCI already started with pid $pid"
    exit 0
  fi
fi

#Just tag this instance path
export _ASR_INSTANCE_FULL_PATH=$PGCI

#Get a UUID. If (rc!=0), simply exit
GETUID_CMD=$PGC_PATH/scripts/getUuid.sh
if [ -f $GETUID_CMD ]
then
  $GETUID_CMD $PGCI
  RC=$?
  [ $RC != 0 ] && exit $RC
fi

usage=`$CMD_PATH -help`
if [[ $usage == *\[-pid\ pidfile\]* ]]; then
    mkdir -p $INSTALL_DIR/var/tmp/pids/
    $CMD_PATH -r $INSTALL_DIR -h localhost -p _PF_ -g _GRP_ -c _COMP_ -i "$1" -d $PGCI_PATH/.config -pid $PIDFILE
else
    $CMD_PATH -r $INSTALL_DIR -h localhost -p _PF_ -g _GRP_ -c _COMP_ -i $1 -d $PGCI_PATH/.config > /dev/null 2>&1 &
    pid=$!
    echo ${pid} > $PIDFILE
    sed -i -e "s/^instance.pid=.*/instance.pid=${pid}/" $PGCI_PATH/.config/system.cfg
fi

