#!/bin/bash
actions() {
  debug=false
  ./start.sh -l $CASR_LOGGER
  while [ -e "/casr/debug" ]; do
    debug=true
    sleep 5
  done
}

actions
while $debug; do
  actions
done