#!/bin/bash

CP=$TESTDIR/bundles/*
java -cp "$CP" -Dreport.file=TEST-DIAMETER.xml -Dserver=127.0.0.1 -Dport=3868 -DtestName=com.alcatel.as.diameter.agent.stest.MainTest com.nokia.as.util.test.player.TestPlayer .
