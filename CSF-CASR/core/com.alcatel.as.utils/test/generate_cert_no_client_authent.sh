#!/bin/sh -u


TLSCLIENT=`pwd`/client.ks
TLSSERVER=`pwd`/server.ks
TLSSERVERTRUST=`pwd`/server.trust

##
# Generate *Server* Public/Private Keys
#
$JAVA_HOME/bin/keytool -genkey -keyalg rsa -keystore $TLSSERVER -alias server <<EOF
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
$JAVA_HOME/bin/keytool -genkey -keyalg rsa -keystore $TLSCLIENT -alias client <<EOF
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
# Import *Server* public key into the Client keystore 
#
$JAVA_HOME/bin/keytool -export -alias server -keystore $TLSSERVER -file $TLSSERVERTRUST <<EOF
password
EOF

$JAVA_HOME/bin/keytool -import -alias server -keystore $TLSCLIENT -file $TLSSERVERTRUST <<EOF
password
yes
EOF

