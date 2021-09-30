#!/bin/bash

curl -v -I -X GET --proxy http://127.0.0.1:8090 http://127.0.0.1:9999/test | grep "HTTP/1.1 200 OK"



