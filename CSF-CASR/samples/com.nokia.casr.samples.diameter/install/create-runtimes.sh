#!/bin/bash

# This script is used to create two runtimes, one for the client jvm, and one for the server jvm.

#
# Initialization.
#
function init() {
    myscriptname=$1
    dir=`which $myscriptname`
    dir=`dirname $dir`
    dir=`(unset CDPATH ; cd $dir ; pwd)`
    INSTALLDIR=$dir
    RUNTIMEDIR=$dir/../runtime
    TOPDIR=$dir/..
}

#
# Wait for the end of a process, and display a progress bar
#
function waitForProcess() {
    spin='-\|/'
    i=0
    while kill -0 $pid 2>/dev/null
    do
	i=$(( (i+1) %4 ))
	printf "\r${spin:$i:1}"
	sleep .1
    done
    echo
}

#
# Download "microfeature tool", it will be used to create a runtime which will be used for both client and server
#
function downloadLatestMicrofeatureTool() {
    echo "Downloading microfeature ..."
    cd /tmp
    wget -q -O com.nokia.casr.microfeatures.main.jar http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered/com/nokia/casr/microfeatures/com.nokia.casr.microfeatures.main/\[RELEASE\]/com.nokia.casr.microfeatures.main-\[RELEASE\].jar &
    pid=$! # Process Id of the previous running command
    [ $? -ne 0 ] && echo "Could not download http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered/com/nokia/casr/microfeatures/com.nokia.casr.microfeatures.main/\[RELEASE\]/com.nokia.casr.microfeatures.main-\[RELEASE\].jar" && exit 1
    waitForProcess $pid
}

#
# Creates a runtime to /tmp/cjdi-runtime-1.0.0.zip
# 
function createRuntimes() {
    cd /tmp
    echo "Creating cjdi runtime, this operation may take a while (artifacts are resolved and downloaded from remote artifactory server). In case of errors, check /tmp/microfeatures.log ..."

    # create a runtime using latest OBR version with the following features: felix(log4j1); agent diameter proxylet, ioh diameter, aries jmx
    java -Dcreate=cjdi-runtime,1.0.0,runtime.felix.log4j1,agent.proxylet.diameter,ioh.mux.diameter,lib.aries.jmx -jar com.nokia.casr.microfeatures.main.jar > microfeatures.log 2>&1 &

    pid=$! # Process Id of the previous running command
    waitForProcess $pid

    [ -d $RUNTIMEDIR ] && echo "*** $RUNTIMEDIR already exists" && exit 1
    mkdir -p $RUNTIMEDIR
    cd $RUNTIMEDIR
    unzip -q /tmp/cjdi-runtime-1.0.0.zip
    mv cjdi-runtime-1.0.0 client
    cp -r client server
    cp -r client server-spring

    echo 
    echo "Runtimes created. Now configuring client and server runtimes ..."
    cp -f $INSTALLDIR/client-conf/* $RUNTIMEDIR/client/instance/.
    cp -f $INSTALLDIR/server-conf/* $RUNTIMEDIR/server/instance/.
    cp -f $INSTALLDIR/server-conf/* $RUNTIMEDIR/server-spring/instance/.
}

#
# copy artifacts to client and server runtimes
#
function installBundles() {
    echo "Copying client demo to client runtime ..." 
    cp -f $TOPDIR/client/target/com.nokia.casr.samples.diameter.client-*.jar $RUNTIMEDIR/client/bundles/
    echo "Copying server demo to server runtimes ..." 
    cp -f $TOPDIR/server/target/com.nokia.casr.samples.diameter.server-*.jar $RUNTIMEDIR/server/bundles/
    echo "Copying server-spring demo to server-spring runtime ..." 
    cp -f $TOPDIR/server-spring/target/com.nokia.casr.samples.diameter.server.spring-*.jar $RUNTIMEDIR/server-spring/bundles/
}

init $0
downloadLatestMicrofeatureTool
createRuntimes
installBundles

echo
echo "All installed, from one console, you can now start the client like this ->"
echo
echo "  cd runtime/client/; ./start.sh -l"
echo "  (the client jvm is started in the forground and logs are displayed to stdout)"

echo 
echo "Now, you can start either the server or the server-spring server:"
echo ""

echo "To start the server from another console ->"
echo
echo "  cd runtime/server; ./start.sh -l"
echo "  (the server jvm is started in the forground and logs are displayed to stdout)"
echo ""

echo "To start the server-spring from another console ->"
echo
echo "  cd runtime/server-spring; ./start.sh -l"
echo "  (the server-spring jvm is started in the forground and logs are displayed to stdout)"



