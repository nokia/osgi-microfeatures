#!/bin/bash

curl -v --noproxy "*" http://127.0.0.1:8080/test 2>&1|grep "HTTP/1.1 200 OK"





