#!/bin/bash

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
server.alcatel.com c
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

# copy client certificate in server/ conf dir
cp -f tls/server.ks /tmp/server-keystore.ks




