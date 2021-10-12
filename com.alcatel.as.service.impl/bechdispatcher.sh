# Copyright 2000-2021 Nokia
#
# Licensed under the Apache License 2.0
# SPDX-License-Identifier: Apache-2.0
#

java -server -Xmx1024m -Xms1024m -cp /opt/proxy/resource:classes:lib/platform-service-impl.jar -Djava.ext.dirs=lib:/opt/proxy/bundles/custo:/opt/proxy/lib testing.EventDispatcherBench 100 10000 1024 0 tpool
#java -agentlib:hprof=cpu=samples,monitor=y,depth=20 -Xmx1024m -Xms1024m -cp /opt/proxy/resource:classes:lib/platform-service-impl.jar -Djava.ext.dirs=lib::/opt/proxy/bundles/custo:/opt/proxy/lib testing.EventDispatcherBench 100 10000 1024 0 pfexec
