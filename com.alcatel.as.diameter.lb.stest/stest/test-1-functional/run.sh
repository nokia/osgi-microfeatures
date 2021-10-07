#!/bin/bash

RUNTIME=$TESTDIR/CDLB-runtime-0.0.0
CP=$TESTDIR/bundles/*

java -cp "$CP" -Dserver=127.0.0.1 -Dport=3868 -DtestName=com.alcatel.as.diameter.lb.stest.DiameterLBTest com.nokia.as.util.test.player.TestPlayer .
