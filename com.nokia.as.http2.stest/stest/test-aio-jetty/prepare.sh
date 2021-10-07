#!/bin/bash

trap 'exit 1' ERR

echo "Waiting for JETTY/Jersey Server to be ready..."


wait_str() {
  local file="$1"; shift
  local search_term="$1"; shift
  local wait_time="${1:-5m}";
  
  (timeout $wait_time tail -Fn +1 "$file" &) | grep -qEi "$search_term" && return 0
  echo "Timeout of $wait_time reached. Unable to find '$search_term' in '$file'"
  exit 1
}

wait_str ../runtimes/aio-jetty/var/log/csf.runtime__component.instance/msg.log "initialized." 30
sleep 2
echo "JETTY/Jersey Ready"
