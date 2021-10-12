# Copyright 2000-2021 Nokia
#
# Licensed under the Apache License 2.0
# SPDX-License-Identifier: Apache-2.0
#

# Stops the Diameter LB standalone server
## --------------------------------------

while getopts ":r:h:p:g:c:i:d:" opt; do
  case $opt in
    r) ASR_ROOT=$OPTARG
    ;;
    h) HOST=$OPTARG
    ;;
    p) PLATFORM=$OPTARG
    ;;
    g) GROUP=$OPTARG
    ;;
    c) COMPONENT=$OPTARG
    ;;
    i) INSTANCE=$OPTARG
    ;;
    d) CONFIG_DIR=$OPTARG
    ;;
    \?) echo "Invalid option: -$OPTARG" && exit 1
    ;;
    :) echo "Option -$OPTARG requires an argument." && exit 1
    ;;
  esac
done

kill -9 `cat ${ASR_ROOT}/var/tmp/pids/${INSTANCE}.pid`

