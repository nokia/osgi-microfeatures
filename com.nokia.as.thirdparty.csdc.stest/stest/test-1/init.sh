#!/bin/bash

DOWNLOAD_URL="${CSF_REPO_DELIVERED:-https://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered}/com/nokia/casr/tpl"
ETCD=etcd-v3.3.13-linux-amd64
ETCD_VER=v3.3.13
ETCD_URL=${DOWNLOAD_URL}/etcd-${ETCD_VER}-linux-amd64.tar.gz

download_etcd() {
    if [ ! -d /tmp/${ETCD} ]; then
		wget -O /tmp/${ETCD}.tar.tgz $ETCD_URL
		cd /tmp
		tar zxvf ${ETCD}.tar.tgz
    fi
}

# Download and start ETCD, which contains zookeeper + kafka + scheme registry
download_etcd

# start zookeeper, kafka server, and schema registry
cd /tmp/${ETCD}
echo "Starting etcd ..."
./etcd &
if [ $? -ne 0 ]; then
    echo "could not start zoo keeper"
    exit 1
fi
PID=$!
echo $PID > pid.txt
echo "PID=`cat pid.txt`"
sleep 2


