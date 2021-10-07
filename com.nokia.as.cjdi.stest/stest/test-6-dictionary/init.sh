#!/bin/bash

echo "===== Diameter Init Script ======"
echo "diameteragent.originstateid=123456789" >> server/instance/diameteragent.cfg
sed -i 's/diameteragent\.firmwareRevision\=.*/diameteragent\.firmwareRevision\=4/g' client/instance/diameteragent.cfg
sed -i 's/diameteragent\.originHost\=.*/diameteragent\.originHost\=client.nokia.com/g' client/instance/diameteragent.cfg
echo "diameteragent.originstateid=0" >> client/instance/diameteragent.cfg
echo "diameteragent.rfc3539.twinit=5" >> client/instance/diameteragent.cfg
echo "diameteragent.vendorId=1234" >> client/instance/diameteragent.cfg 
echo "-Djunit4osgi.test=com.nokia.as.cjdi.stest.client.DictionaryClientTest" > client/instance/user.jvm.opt    
