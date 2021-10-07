#!/bin/bash

curl -v -I --http2-prior-knowledge --proxy http://127.0.0.1:8090 http://127.0.0.1:9999/test | grep "HTTP/2 200"




