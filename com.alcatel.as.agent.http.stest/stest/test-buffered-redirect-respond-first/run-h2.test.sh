#!/bin/bash
# Copyright 2000-2021 Nokia
#
# Licensed under the Apache License 2.0
# SPDX-License-Identifier: Apache-2.0
#


curl -v -I --http2-prior-knowledge --proxy http://127.0.0.1:8090 http://127.0.0.1:9999/test | grep "HTTP/2 200"




