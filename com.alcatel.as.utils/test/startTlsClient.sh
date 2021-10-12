# Copyright 2000-2021 Nokia
#
# Licensed under the Apache License 2.0
# SPDX-License-Identifier: Apache-2.0
#

CP=tlsclient:resource
for i in `ls lib/*.jar /opt/proxy/lib/*.jar`; do
    CP=$CP:$i
done

OPTS="\
    -Djavax.net.ssl.trustStore=test/client.ks/keystore \
    -Djavax.net.ssl.trustStorePassword=password"
java -cp $CP $OPTS alcatel.tess.hometop.gateways.reactor.examples.TestTcpClientSecure 127.0.0.1 9999
