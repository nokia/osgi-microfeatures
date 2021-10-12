#!/bin/bash
# Copyright 2000-2021 Nokia
#
# Licensed under the Apache License 2.0
# SPDX-License-Identifier: Apache-2.0
#


echo "===== Diameter Init Script ======"
echo "diameteragent.originstateid=123456789" >> server/instance/diameteragent.cfg
sed -i 's/diameteragent\.firmwareRevision\=.*/diameteragent\.firmwareRevision\=4/g' client/instance/diameteragent.cfg
sed -i 's/diameteragent\.originHost\=.*/diameteragent\.originHost\=client.nokia.com/g' client/instance/diameteragent.cfg
echo "diameteragent.originstateid=0" >> client/instance/diameteragent.cfg
echo "diameteragent.rfc3539.twinit=5" >> client/instance/diameteragent.cfg
echo "diameteragent.vendorId=1234" >> client/instance/diameteragent.cfg 
echo "-Djunit4osgi.test=com.nokia.as.cjdi.stest.client.BasicClientSctpTest" > client/instance/user.jvm.opt    
