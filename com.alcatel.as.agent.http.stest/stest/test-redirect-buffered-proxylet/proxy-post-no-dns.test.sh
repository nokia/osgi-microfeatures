trap 'exit 1' ERR

content=`mktemp`
dd if=/dev/zero of=$content bs=1k count=1024
headers=`mktemp`
response=`mktemp`
curl -v --noproxy "*" -D $headers -F  "my_file=@$content"  http://127.0.0.1:8088/services/helloworld > $response
grep 'X-Hello: World' $headers
grep 'header OK: true' $response
grep 'File size: 1048576' $response
echo "OK"
