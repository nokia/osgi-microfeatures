#!/bin/bash
# Copyright 2000-2021 Nokia
#
# Licensed under the Apache License 2.0
# SPDX-License-Identifier: Apache-2.0
#

err_report() {
    echo "Error on line $1"
    exit 1
}

trap 'err_report $LINENO' ERR

wait_str() {
  local file="$1"; shift
  local search_term="$1"; shift
  local wait_time="${1:-5m}";

  (timeout $wait_time tail -Fn +1 "$file" &) | grep -qEi "$search_term" && return 0
  echo "Timeout of $wait_time reached. Unable to find '$search_term' in '$file'"
  exit 1
}

unset no_proxy
unset NO_PROXY

response=`mktemp`
curl -v -X DELETE --proxy "http://localhost:3128" http://localhost:8088/services/helloworld/delete > $response
grep 'Hello DELETE' $response

wait_str ../runtimes/proxy/var/log/csf.runtime__component.instance/msg.log "Hello DELETE" 20
echo "OK"
