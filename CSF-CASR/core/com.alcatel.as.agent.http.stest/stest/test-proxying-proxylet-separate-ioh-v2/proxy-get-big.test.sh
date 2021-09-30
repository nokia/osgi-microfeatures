trap 'exit 1' ERR


response=`mktemp`
curl --proxy "http://localhost:8088"   http://localhost:8080/services/helloworld/big/ -v > $response
du -b $response | grep 65536
echo "OK"
