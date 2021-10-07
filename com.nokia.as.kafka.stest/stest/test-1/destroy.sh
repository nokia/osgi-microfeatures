#!/bin/bash

# shutdown zookeeper, kafka server, and schema registry

CONFLUENCE=confluent-5.2.1
CONFLUENCE_URL=http://packages.confluent.io/archive/5.2/confluent-5.2.1-2.12.tar.gz

cd /tmp/${CONFLUENCE}
echo "Stopping schema registry ..."
./bin/schema-registry-stop -daemon
if [ $? -ne 0 ]; then
    echo "could not stop schema registry"
else
    sleep 5
fi

echo "Stopping schema kafka server ..."
./bin/kafka-server-stop -daemon
if [ $? -ne 0 ]; then
    echo "could not stop kafka"
else
    sleep 15
fi

echo "Stopping zookeeper ..."
./bin/zookeeper-server-stop -daemon
if [ $? -ne 0 ]; then
    echo "could not stop zoo keeper"
fi
exit 0


