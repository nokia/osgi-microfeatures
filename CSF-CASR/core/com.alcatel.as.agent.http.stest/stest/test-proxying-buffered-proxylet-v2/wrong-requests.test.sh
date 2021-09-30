trap 'exit 1' ERR


response=`mktemp`
curl -v --proxy "http://localhost:8088"   http://localhost:8111/services/helloworld > $response
grep '50' $response

response=`mktemp`
curl -v --http2-prior-knowledge --proxy "http://localhost:8088"   http://localhost:8111/services/helloworld > $response
grep '50' $response

response=`mktemp`
curl -v --proxy "http://localhost:8088"   http://localhost:8088/services/helloworld > $response
grep '50' $response

response=`mktemp`
curl -v --http2-prior-knowledge --proxy "http://localhost:8088"   http://localhost:8088/services/helloworld > $response
grep '50' $response

response=`mktemp`
curl -v --proxy "http://localhost:8088"   http://dsfargeggg:8088/services/helloworld > $response
grep '50' $response

response=`mktemp`
curl -v --http2-prior-knowledge --proxy "http://localhost:8088"   http://dsfargeggg:8088/services/helloworld > $response
grep '50' $response

echo "OK"
