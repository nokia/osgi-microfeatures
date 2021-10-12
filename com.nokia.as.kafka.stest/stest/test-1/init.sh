#!/bin/bash
# Copyright 2000-2021 Nokia
#
# Licensed under the Apache License 2.0
# SPDX-License-Identifier: Apache-2.0
#


CONFLUENCE=confluent-5.2.1
CONFLUENCE_URL="${CSF_REPO_DELIVERED:-https://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered}/com/nokia/casr/tpl/confluent-5.2.1-2.12.tar.gz"

download_confluence() {
    if [ ! -d /tmp/${CONFLUENCE} ]; then
	wget -O /tmp/${CONFLUENCE}.tgz $CONFLUENCE_URL
	cd /tmp
	tar zxvf ${CONFLUENCE}.tgz
    fi
}

# Download and start confluence, which contains zookeeper + kafka + scheme registry
download_confluence

# start zookeeper, kafka server, and schema registry
cd /tmp/${CONFLUENCE}
echo "Starting zookeeper ..."
./bin/zookeeper-server-start -daemon etc/kafka/zookeeper.properties
if [ $? -ne 0 ]; then
    echo "could not start zoo keeper"
    exit 1
fi
sleep 2
echo "Starting kafka server ..."
./bin/kafka-server-start -daemon etc/kafka/server.properties
if [ $? -ne 0 ]; then
    echo "could not start kafka"
    exit 1
fi
sleep 10
echo "Starting schema registry ..."
./bin/schema-registry-start -daemon etc/schema-registry/schema-registry.properties
if [ $? -ne 0 ]; then
    echo "could not start schema registry"
    exit 1
fi
sleep 5


