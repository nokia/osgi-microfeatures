#!/bin/bash

curl --noproxy "*" http://127.0.0.1:8080/test | grep "body2"
exit $?

