#!/bin/sh -u


TLS=`pwd`/tls
mkdir -p $TLS
rm -f $TLS/*.ks $TLS/*.cer

##
# Generate *Server* Public/Private Keys
#
$JAVA_HOME/bin/keytool -genkey -keyalg rsa -keystore $TLS/server.ks -alias ccserver -validity 1 -startdate "2015/10/03 00:00:00" <<EOF
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
$JAVA_HOME/bin/keytool -genkey -keyalg rsa -keystore $TLS/client.ks -alias ccclient <<EOF
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
$JAVA_HOME/bin/keytool -export -alias ccclient -keystore $TLS/client.ks -file $TLS/client.cer <<EOF
password
password
EOF

$JAVA_HOME/bin/keytool -import -alias ccclient -keystore $TLS/server.ks -file $TLS/client.cer <<EOF
password
yes
EOF

##
# Import *Server* public key into the Client keystore 
#
$JAVA_HOME/bin/keytool -export -alias ccserver -keystore $TLS/server.ks -file $TLS/server.cer <<EOF
password
EOF

$JAVA_HOME/bin/keytool -import -alias ccserver -keystore $TLS/client.ks -file $TLS/server.cer <<EOF
password
yes
EOF

