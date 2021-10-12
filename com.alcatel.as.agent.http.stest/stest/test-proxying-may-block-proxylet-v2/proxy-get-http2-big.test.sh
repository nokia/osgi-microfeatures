# Copyright 2000-2021 Nokia
#
# Licensed under the Apache License 2.0
# SPDX-License-Identifier: Apache-2.0
#

trap 'exit 1' ERR


response=`mktemp`
expectedNum=$RANDOM
curl --http2-prior-knowledge --proxy "http://localhost:8088"   http://localhost:8080/services/helloworld/big -v > $response
du -b $response | grep 65536
echo "OK"

