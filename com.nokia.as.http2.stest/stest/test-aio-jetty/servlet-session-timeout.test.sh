echo "testing session timeout"

trap "exit 1" ERR

COOKIE_JAR="$(mktemp)"
curl  -k --noproxy "*" -c $COOKIE_JAR https://localhost:8443/timeout | grep "1"
curl  -k --noproxy "*" -b $COOKIE_JAR https://localhost:8443/timeout | grep "2"
echo "waiting 5sec, session timeout is 4sec"
sleep 5
curl  -k --noproxy "*" -b $COOKIE_JAR https://localhost:8443/timeout | grep "1"
echo "OK"