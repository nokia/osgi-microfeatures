# Copyright 2000-2021 Nokia
#
# Licensed under the Apache License 2.0
# SPDX-License-Identifier: Apache-2.0
#

trap 'exit 1' ERR


sleep 1
response=`mktemp`
expectedNum=$RANDOM
curl -v --http2-prior-knowledge  http://localhost:8088/services/helloworld?$expectedNum > $response
grep "\"GET /services/helloworld?1337 HTTP/2.0\" 200" ../runtimes/jersey-server/var/log/csf.runtime__component.instance/msg.log
echo "OK"