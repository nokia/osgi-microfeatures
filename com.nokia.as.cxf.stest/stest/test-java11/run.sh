#!/bin/bash

headers=`mktemp`
body=`mktemp`
trap "rm -f $headers $body" EXIT

curl --noproxy "*" -D $headers http://127.0.0.1:8080/soap/hello?wsdl > $body

grep 'HTTP/1.1 200 OK' $headers
ok=$?
if [ "$ok" != "0" ]; then
    echo "test failed, wrong status"
    cat $headers
    exit 1;
fi

grep 'Content-Type: text/xml;charset=utf-8' $headers
ok=$?
if [ "$ok" != "0" ]; then
    echo "test failed, wrong content type"
    cat $headers
    exit 1;
fi

cat $headers
cat $body
exit 0






