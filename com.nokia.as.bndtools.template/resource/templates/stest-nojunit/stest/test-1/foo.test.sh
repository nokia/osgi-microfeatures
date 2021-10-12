#!/bin/bash
# Copyright 2000-2021 Nokia
#
# Licensed under the Apache License 2.0
# SPDX-License-Identifier: Apache-2.0
#


# at this point, the init.sh script has been called, then the runtimes have been started
#(but only the ones for which we have some configuration available, see client/, server/)

#note that all test.sh scripts that start with run will be started by the bash listing order, not just this one!
#The output of this scripts (both stdout and stderr) will be saved in a JUnit report. 

#Do testing stuff here
