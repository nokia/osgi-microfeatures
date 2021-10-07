#!/bin/bash

curl -v -k --noproxy "*" https://localhost:8443/services/helloworld | grep "Hello World!"
