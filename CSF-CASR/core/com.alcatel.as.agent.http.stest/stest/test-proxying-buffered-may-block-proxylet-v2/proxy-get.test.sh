trap 'exit 1' ERR


response=`mktemp`
curl -v  --proxy "http://localhost:8088"   http://localhost:8080/services/helloworld > $response
grep 'Hello Proxylet World' $response
echo "OK"