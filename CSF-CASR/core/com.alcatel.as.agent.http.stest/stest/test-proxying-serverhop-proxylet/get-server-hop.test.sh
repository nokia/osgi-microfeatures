trap 'exit 1' ERR

expectedNum=$RANDOM
curl -v --http2-prior-knowledge -H "X-Server-Hop: true" http://localhost:8088/services/helloworld?$expectedNum
grep "\"GET /services/helloworld?$expectedNum HTTP/2.0\" 200" ../runtimes/jersey-server/var/log/csf.runtime__component.instance/msg.log
grep "jaxrs.jersey2 : connectionAccepted : TcpChannel:.*:8089" ../runtimes/jersey-server/var/log/csf.runtime__component.instance/msg.log


expectedNum=$RANDOM
curl -v -H "X-Server-Hop: true" http://localhost:8088/services/helloworld?$expectedNum
grep "\"GET /services/helloworld?$expectedNum HTTP/1.1\" 200" ../runtimes/jersey-server/var/log/csf.runtime__component.instance/msg.log

echo "OK"