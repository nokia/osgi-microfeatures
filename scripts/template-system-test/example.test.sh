#!/bin/bash

# at this point, the init.sh script has been called, then the runtimes have been started
# (but only the ones for which we have some configuration available)
# now check expected logs from client and server runtimes

# note that all test.sh scripts that start with run will be started by the bash listing order, not just this one!
# The output of this scripts (both stdout and stderr) will be saved in a JUnit report. 

wait_str() {
  local file="$1"; shift
  local search_term="$1"; shift
  local wait_time="${1:-5m}";

  (timeout $wait_time tail -Fn +1 "$file" &) | grep -qEi "$search_term" && return 0
  echo "Timeout of $wait_time reached. Unable to find '$search_term' in '$file'"
  exit 1
}

#the wait_str function can be used to wait for an expected log. use it like this:
#   wait_str ../runtimes/test-runtime/var/log/csf.runtime__component.instance/msg.log "Jersey Ready" 30

#this simple example wait 5 seconds and check if msg.log exists

sleep 5
if [ -f ../runtimes/%FIRST_RUNTIME%/var/log/csf.runtime__component.instance/msg.log ]; then
  echo "msg.log available"
  exit 0
else
  echo "msg.log NOT here"
  exit 1
fi

