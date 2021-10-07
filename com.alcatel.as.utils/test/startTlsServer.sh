CP=tlsserver:resource
for i in `ls lib/*.jar /opt/proxy/lib/*.jar`; do
    CP=$CP:$i
done

#OPTS="-Ddeployment.security.SSLv2Hello=false -Ddeployment.security.SSLv3=false -Ddeployment.security.TLSv1=false"
OPTS="-Djavax.net.debug=tls,cipher"

java -cp $CP $OPTS alcatel.tess.hometop.gateways.reactor.examples.TestTcpServerSecure
