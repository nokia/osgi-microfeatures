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
curl -v --proxy "http://localhost:8088" -D $headers -F  "my_file=@$content"  http://127.0.0.1:8080/services/helloworld > $response
grep 'X-Hello: World' $headers
grep 'header OK: true' $response
grep 'File size: 1048576' $response
echo "OK"
