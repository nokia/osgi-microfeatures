#!/bin/bash
# Copyright 2000-2021 Nokia
#
# Licensed under the Apache License 2.0
# SPDX-License-Identifier: Apache-2.0
#


curl --noproxy "*" http://127.0.0.1:8080/test | grep "body2"
exit $?

