#!/bin/bash
# Copyright 2000-2021 Nokia
#
# Licensed under the Apache License 2.0
# SPDX-License-Identifier: Apache-2.0
#


# OPTIONAL script, called before running tests
# Here, we initialize tls certificates and copy them to the client and server directories

# generate tls certificates
TLS=`pwd`/tls/
mkdir -p $TLS
rm -f $TLS/*.ks $TLS/*.cer 

##
# Generate *Server* Public/Private Keys
#
keytool -genkey -keyalg rsa -keystore $TLS/server.ks -alias ccserver <<EOF
password
password
server.alcatel.com 
Network Access Team
CIT
Massy
IDF
FR
yes

EOF

##
# Generate *Client* Public/Private Keys
#
keytool -genkey -keyalg rsa -keystore $TLS/client.ks -alias ccclient <<EOF
password
password
client.alcatel.com 
Support Team
CIT
Massy
IDF
FR
yes

EOF

##
# Import *Client* public key into the Server keystore
#
keytool -export -alias ccclient -keystore $TLS/client.ks -file $TLS/ccclient.cer <<EOF
password
password
EOF

keytool -import -alias ccclient -keystore $TLS/server.ks -file $TLS/ccclient.cer <<EOF
password
yes
EOF

##
# Import *Server* public key into the Client keystore 
#
keytool -export -alias ccserver -keystore $TLS/server.ks -file $TLS/ccserver.cer <<EOF
password
EOF

keytool -import -alias ccserver -keystore $TLS/client.ks -file $TLS/ccserver.cer <<EOF
password
yes
EOF

# copy client certificate in client/ conf dir
cp -f tls/client.ks client/instance

# copy client certificate in server/ conf dir
cp -f tls/server.ks server/instance

echo "===== Diameter Init Script ======"
echo "diameteragent.originstateid=123456789" >> server/instance/diameteragent.cfg
sed -i 's/diameteragent\.firmwareRevision\=.*/diameteragent\.firmwareRevision\=4/g' client/instance/diameteragent.cfg
sed -i 's/diameteragent\.originHost\=.*/diameteragent\.originHost\=client.nokia.com/g' client/instance/diameteragent.cfg
echo "diameteragent.originstateid=0" >> client/instance/diameteragent.cfg
echo "diameteragent.rfc3539.twinit=5" >> client/instance/diameteragent.cfg
echo "diameteragent.vendorId=1234" >> client/instance/diameteragent.cfg 
echo "-Djunit4osgi.test=com.nokia.as.cjdi.stest.client.BasicClientSctpDtlsTest" > client/instance/user.jvm.opt    




