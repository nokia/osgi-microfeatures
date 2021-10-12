# Copyright 2000-2021 Nokia
#
# Licensed under the Apache License 2.0
# SPDX-License-Identifier: Apache-2.0
#

trap 'exit 1' ERR

content=`mktemp`
dd if=/dev/zero of=$content bs=1k count=1024
headers=`mktemp`
response=`mktemp`
expectedNum=$RANDOM
curl -v --http2-prior-knowledge --proxy "http://localhost:8088" -D $headers -F  "my_file=@$content"  http://127.0.0.1:8080/services/helloworld?$expectedNum > $response
grep 'x-hello: World' $headers
grep 'header OK: true' $response
grep 'File size: 1048576' $response
grep "\"POST /services/helloworld?$expectedNum HTTP/2.0\" 200" ../runtimes/jersey-server/var/log/csf.runtime__component.instance/msg.log
echo "OK"
