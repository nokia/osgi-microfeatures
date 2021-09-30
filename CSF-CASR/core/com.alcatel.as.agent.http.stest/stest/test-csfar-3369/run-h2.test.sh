#!/bin/bash

curl --http2-prior-knowledge --noproxy "*" http://127.0.0.1:8080/test | grep "body2"
exit $?

