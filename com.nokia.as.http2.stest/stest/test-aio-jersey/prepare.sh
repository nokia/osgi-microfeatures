#!/bin/bash
# Copyright 2000-2021 Nokia
#
# Licensed under the Apache License 2.0
# SPDX-License-Identifier: Apache-2.0
#


trap 'exit 1' ERR

echo "Waiting for Jersey Server to be ready..."


wait_str() {
  local file="$1"; shift
  local search_term="$1"; shift
  local wait_time="${1:-5m}";
  
  (timeout $wait_time tail -Fn +1 "$file" &) | grep -qEi "$search_term" && return 0
  echo "Timeout of $wait_time reached. Unable to find '$search_term' in '$file'"
  exit 1
}

wait_str ../runtimes/aio-jersey/var/log/csf.runtime__component.instance/msg.log "initialized." 30
# TODO remove
sleep 2
echo "Jersey Ready"
