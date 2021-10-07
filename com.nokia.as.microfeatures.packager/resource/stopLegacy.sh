#!/bin/sh
# Auto generated wrapper script for scripts/stop.sh
echo stop _PF_/_GRP_/_COMP_/$1

PGC=_PF_/_GRP_/_COMP_
PGCI=$PGC/$1
PGC_PATH=$INSTALL_DIR/localinstances/$PGC
PGCI_PATH=$INSTALL_DIR/localinstances/$PGCI
PIDFILE=$PGCI_PATH/.pid
CMD_PATH=$PGC_PATH/scripts/stop.sh
$CMD_PATH -r $INSTALL_DIR -h localhost -p _PF_ -g _GRP_ -c _COMP_ -i $1 -d $PGCI_PATH/.config > /dev/null 2>&1
if [ -f $PIDFILE ]; then
    kill -9 `cat ${PIDFILE}` > /dev/null 2>&1
fi
