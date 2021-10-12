#!/bin/bash
# Copyright 2000-2021 Nokia
#
# Licensed under the Apache License 2.0
# SPDX-License-Identifier: Apache-2.0
#


# shutdown zookeeper, kafka server, and schema registry

ETCD=etcd-v3.3.13-linux-amd64

cd /tmp/${ETCD}
echo "Stopping etcd ..."
kill -9 `cat pid.txt`
exit 0


