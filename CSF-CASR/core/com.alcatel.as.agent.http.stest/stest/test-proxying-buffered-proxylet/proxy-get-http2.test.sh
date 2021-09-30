trap 'exit 1' ERR


response=`mktemp`
expectedNum=$RANDOM
curl -v  --http2-prior-knowledge --proxy "http://localhost:8088"   http://localhost:8080/services/helloworld?$expectedNum > $response
grep "\"GET /services/helloworld?$expectedNum HTTP/2.0\" 200" ../runtimes/jersey-server/var/log/csf.runtime__component.instance/msg.log
echo "OK"