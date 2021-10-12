# Copyright 2000-2021 Nokia
#
# Licensed under the Apache License 2.0
# SPDX-License-Identifier: Apache-2.0
#

trap 'exit 1' ERR

content=`mktemp`
dd if=/dev/zero of=$content bs=1k count=16
headers=`mktemp`
response=`mktemp`
expectedNum=$RANDOM
curl -v --noproxy "*"  --http2-prior-knowledge -D $headers  -F  "my_file=@$content"  http://localhost:8088/services/helloworld?$expectedNum > $response
grep 'x-hello: World' $headers
grep 'header OK: true' $response
grep 'File size: 16384' $response
grep "\"POST /services/helloworld?$expectedNum HTTP/2.0\" 200" ../runtimes/jersey-server/var/log/csf.runtime__component.instance/msg.log
echo "OK"
