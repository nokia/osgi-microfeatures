#!/bin/bash
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

content=`mktemp`
dd if=/dev/zero of=$content bs=1k count=16
headers=`mktemp`
response=`mktemp`
curl  -v --proxy "http://localhost:3128" -H "X-Alexa: play-despacito" -F  "my_file=@$content"  http://localhost:8088/services/helloworld > $response
grep 'File size: 16384' $response
echo "OK"
