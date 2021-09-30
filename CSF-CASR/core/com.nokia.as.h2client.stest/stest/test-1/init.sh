#!/bin/bash

KT_OPTION='-J-Duser.language=en-US'
KT="$JAVA_HOME/bin/keytool"

rm -f /tmp/client.pkcs12
rm -f /tmp/server.pkcs12
"$KT" $KT_OPTION -genkey -keyalg RSA -alias selfsigned -keystore /tmp/server.pkcs12 -storepass password -validity 36000 -keysize 2048 -storetype pkcs12 << EOF
1
2
3
4
5
6
yes

EOF

"$KT" $KT_OPTION -exportcert -v -alias selfsigned -keystore /tmp/server.pkcs12 -storepass password -storetype pkcs12 -file /tmp/server.cert

"$KT" $KT_OPTION -importcert -alias selfsigned -keystore /tmp/client.pkcs12 -storepass password -storetype pkcs12 -file /tmp/server.cert << EOF
yes
EOF

rm /tmp/server.cert


"$KT" $KT_OPTION -list -v -keystore /tmp/server.pkcs12 -storepass password -storetype pkcs12
"$KT" $KT_OPTION -list -v -keystore /tmp/client.pkcs12 -storepass password -storetype pkcs12 

