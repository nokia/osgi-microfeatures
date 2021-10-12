#!/bin/bash
# Copyright 2000-2021 Nokia
#
# Licensed under the Apache License 2.0
# SPDX-License-Identifier: Apache-2.0
#


if [ $# -ne 1 ]; then
    echo "Usage: $O <test dir>"
    return
fi

name=$1

# determine root directory (on top of system-tests, where there is also the runtime dir)
SCRIPTDIR=`pwd`
cd $SCRIPTDIR/..

# set TOPDIR which points to root dir (on top of runtime and scripts-dir)
export TOPDIR=`pwd`
export TESTDIR=${TOPDIR}/system-tests/$name
export AS_JUNIT4OSGI_REPORTSDIR=${TOPDIR}/system-tests/test-reports/junit4OSGi

cd - > /dev/null 





