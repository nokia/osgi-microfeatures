trap 'exit 1' ERR


response=`mktemp`
curl -v --proxy "http://localhost:8088" -v http://localhost:8080/services/helloworld > $response
grep "Hello Proxylet World" $response
echo "ioh 1 OK"

response=`mktemp`
curl -v --proxy "http://localhost:8188" -v http://localhost:8080/services/helloworld > $response
grep "Hello Proxylet World" $response

echo "ioh 2 OK"
