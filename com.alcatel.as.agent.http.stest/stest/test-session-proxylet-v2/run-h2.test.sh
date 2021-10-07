#!/bin/bash

echo "testing session timeout"

trap "exit 1" ERR

COOKIE_JAR="$(mktemp)"
curl --noproxy "*" -H "X-Next-Port: 8080" -c $COOKIE_JAR --http2-prior-knowledge http://localhost:8088/timeout | grep "Counter: 1"
curl --noproxy "*" -H "X-Next-Port: 8080" -b $COOKIE_JAR --http2-prior-knowledge http://localhost:8088/timeout | grep "Counter: 2"
echo "waiting 5sec, session timeout is 5sec"
sleep 6
curl  --noproxy "*" -H "X-Next-Port: 8080" -b $COOKIE_JAR --http2-prior-knowledge http://localhost:8088/timeout | grep "Counter: 1"
echo "OK"
