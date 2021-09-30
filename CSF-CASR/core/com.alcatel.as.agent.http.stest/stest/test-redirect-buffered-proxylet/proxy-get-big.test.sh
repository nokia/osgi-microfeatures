trap 'exit 1' ERR


response=`mktemp`
curl --noproxy "*" http://localhost:8088/services/helloworld/big/ -v > $response
du -b $response | grep 65536
echo "OK"
