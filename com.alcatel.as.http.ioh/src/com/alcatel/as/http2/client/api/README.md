# Things to test

## When receiving an HTTP2 Reset Frame from the origin server

-> notified in abortRequest
 -> cf is completedExceptionnaly with java.net.ProtocolException


###

Http2ResponseListenerImpl
we don't need cf_body_subscriber

having subscriber should be enough