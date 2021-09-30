trap 'exit 1' ERR

content=`mktemp`
dd if=/dev/zero of=$content bs=1k count=1024
headers=`mktemp`
response=`mktemp`
expectedNum=$RANDOM
curl -v --noproxy "*"  --http2-prior-knowledge -D $headers -F  "my_file=@$content"  http://127.0.0.1:8088/services/helloworld?$expectedNum > $response
grep 'x-hello: World' $headers
grep 'header OK: true' $response
grep 'File size: 1048576' $response
grep "\"POST /services/helloworld?$expectedNum HTTP/2.0\" 200" ../runtimes/jersey-server/var/log/csf.runtime__component.instance/msg.log
echo "OK"
