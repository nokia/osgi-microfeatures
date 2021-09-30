trap 'exit 1' ERR


response=`mktemp`
curl -v --noproxy "*"  http://localhost:8088/services/helloworld > $response
grep 'Hello Proxylet World' $response
echo "OK"