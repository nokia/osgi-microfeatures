#!/bin/bash

# at this point, the init.sh script has been called, then the runtimes have been started (but only the ones for which we have some configuration available, see client/, server/)
# now check expected logs from client and server runtimes

wait_str() {
  local file="$1"; shift
  local search_term="$1"; shift
  local wait_time="${1:-5m}";

  (timeout $wait_time tail -Fn +1 "$file" &) | grep -qEi "$search_term" && return 0
  echo "Timeout of $wait_time reached. Unable to find '$search_term' in '$file'"
  exit 1
}

wait_str ../runtimes/test-agent-tls/var/log/csf.runtime__component.instance/msg.log "Junit4Osgi: Tests done: passed: 3" 15

