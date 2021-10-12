#!/bin/bash
# Copyright 2000-2021 Nokia
#
# Licensed under the Apache License 2.0
# SPDX-License-Identifier: Apache-2.0
#


curl -k --noproxy "*" https://localhost:8443/services/helloworld | grep "Hello World!"
