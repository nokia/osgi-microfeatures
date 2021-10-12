# Copyright 2000-2021 Nokia
#
# Licensed under the Apache License 2.0
# SPDX-License-Identifier: Apache-2.0
#

trap 'exit 1' ERR

expectedNum=$RANDOM
curl -v http://localhost:8088/services/helloworld?$expectedNum
grep "\"GET /services/helloworld?$expectedNum HTTP/2.0\" 200" ../runtimes/jersey-server/var/log/csf.runtime__component.instance/msg.log

expectedNum=$RANDOM
curl -v --http2-prior-knowledge http://localhost:8088/services/helloworld?$expectedNum
grep "\"GET /services/helloworld?$expectedNum HTTP/1.1\" 200" ../runtimes/jersey-server/var/log/csf.runtime__component.instance/msg.log

echo "OK"