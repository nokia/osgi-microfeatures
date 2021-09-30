#!/bin/bash

echo "testing session timeout"

trap "exit 1" ERR

COOKIE_JAR="$(mktemp)"
curl -v --noproxy "*" -H "X-Counter: 1" -c $COOKIE_JAR http://localhost:8080/timeout | grep "Counter: 1"
curl -v --noproxy "*" -H "X-Counter: 2" -b $COOKIE_JAR http://localhost:8080/timeout | grep "Counter: 2"
echo "waiting 5sec, session timeout is 5sec"
sleep 6
curl  -v --noproxy "*" -H "X-Counter: 3" -b $COOKIE_JAR http://localhost:8080/timeout | grep "Counter: 1"
echo "OK"






