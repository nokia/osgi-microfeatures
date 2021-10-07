package com.nokia.as.h2client.test.client;

import com.alcatel.as.http2.client.api.HttpRequest;
import com.alcatel.as.http2.client.api.HttpResponse;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class JaxRsTestSuite implements TestSuit {


    abstract class JaxRsParenttTestDescriptor implements Http2ClientTestDescriptor<String> {
        String GET_URI = "http://127.0.0.1:9092/services/test/generator";
        String POST_URI   = "http://127.0.0.1:9092/services/test/echo";
        String content = "{  \"ipv4Addr\":\"10.182.119.20\", \"ipv6Prefix\": \"2001:0db8:85a3:0000:0000:8a2e:0370:7334\", \"macAddr48\": \"00-14-22-01-23-45\", " +
                "\"supi\": \"284011234567890\", \"gpsi\": \"9876543210\", \"ipDomain\": \"ipdomains\", \"dnn\": \"internet.mnc012.mcc345.gprs\", \"pcfFqdn\": \"nsn.com\", \"pcfIpEndPoints\": [{" +
                " \"ipv4Address\": \"10.0.0.100\", \"ipv6Address\": \"2001:0db8:85a3:0000:0000:8a2e:0370:7334\", \"transport\": \"TCP\", \"port\": 9999 }], \"pcfDiamHost\": \"nsn.example.com\"," +
                " \"pcfDiamRealm\": \"nsn.realm.com\", \"snssai\": { \"sst\": 1, \"sd\": \"19CDE0\" }}";

        @Override
        public HttpRequest.Builder buildRequest(Context context, HttpRequest.Builder builder) {

            return builder
                    .uri(URI.create(POST_URI))
                    .timeout(java.time.Duration.ofSeconds(3))
                    .header("some-bad-Header", "application/json")
                    .header("some-plain-header", "application/json")
                    .header("content-type", "application/json")
                    ;
        }

        @Override
        public HttpResponse.BodyHandler<String> bodyHandler(Context context) {
            return context.getFactory().bodyHandlers().ofString();
        }

        public boolean checkContainsContent(Context context, String body) {
            return body.contains(content);
        }
        public boolean checkContainsContentType(Context context, String body) {
            return body.contains("content-type") && body.contains("application/json") ;
        }

        public boolean checkContainsContentLength(Context context, String body) {
            return body.contains("content-length") ;
        }

    }

    @Override
    public List<Http2ClientTestDescriptor> tests() {
        return Collections.unmodifiableList(
                Arrays.asList(new Http2ClientTestDescriptor[]{
                        new PostEchoJsonLength()
                        ,new PostEchoJsonStream()
                        ,new PostEchoEmptyString()
                        ,new PostEchoNoBody()
                        ,new Delete204()
                })
        );
    }

    class PostEchoJsonLength extends JaxRsParenttTestDescriptor {
        @Override
        public HttpRequest.Builder buildRequest(Context context, HttpRequest.Builder builder) {
            return super.buildRequest(context, builder)
                    .POST(context.getFactory().bodyPublishers().ofString(content))
                    ;
        }

        @Override
        public boolean checkContent(Context context, String body) {
            return
                    checkContainsContent(context, body)
                            && checkContainsContentType(context,body)
                            && checkContainsContentLength(context,body)
                    ;
        }
        @Override
        public String name() {
            return "POST-ECHO-json-length";
        }
    }

    class PostEchoJsonStream extends JaxRsParenttTestDescriptor {
        @Override
        public HttpRequest.Builder buildRequest(Context context, HttpRequest.Builder builder) {
            InputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

            return super.buildRequest(context, builder)
                    .POST(context.getFactory().bodyPublishers().ofInputStream(() -> inputStream))
                    ;
        }

        @Override
        public boolean checkContent(Context context, String body) {
            return
                    checkContainsContent(context, body)
                            && checkContainsContentType(context,body)
                            && ! checkContainsContentLength(context,body)
                    ;
        }
        @Override
        public String name() {
            return "POST-ECHO-json-stream";
        }
    }

    class PostEchoEmptyString extends JaxRsParenttTestDescriptor {
        @Override
        public HttpRequest.Builder buildRequest(Context context, HttpRequest.Builder builder) {
            return super.buildRequest(context, builder)
                    .POST(context.getFactory().bodyPublishers().ofString(""))
                    ;
        }

        @Override
        public boolean checkContent(Context context, String body) {
            return
                            checkContainsContentType(context,body)
                            && ! checkContainsContentLength(context,body)
                    ;
        }
        @Override
        public String name() {
            return "POST-ECHO-empty-string";
        }
    }

    class PostEchoNoBody extends JaxRsParenttTestDescriptor {
        @Override
        public HttpRequest.Builder buildRequest(Context context, HttpRequest.Builder builder) {
            return super.buildRequest(context, builder)
                    .POST(context.getFactory().bodyPublishers().noBody())
                    ;
        }

        @Override
        public boolean checkContent(Context context, String body) {
            return
                            checkContainsContentType(context,body)
                            && ! checkContainsContentLength(context,body)
                    ;
        }
        @Override
        public String name() {
            return "POST-ECHO-no-body";
        }
    }

    class Delete204 extends JaxRsParenttTestDescriptor {
        @Override
        public HttpRequest.Builder buildRequest(Context context, HttpRequest.Builder builder) {
            return builder
                    .uri(URI.create(GET_URI))
                    .DELETE()
                    ;
        }

        @Override
        public boolean checkStatusCode(Context context, int statusCode) {
            return statusCode == 204;
        }

        @Override
        public boolean checkContent(Context context, String body) {
            if (body==null) {
                context.getLogger().warn("Content is null!");
            } else {
                if (body.isEmpty())
                    context.getLogger().warn("Content is empty! because we used bodyHandlers().ofString()");
                else {
                    context.getLogger().error("Content is surprising:"+content);
                    return false;
                }
            }
            return true;
        }

        @Override
        public String name() {
            return "DELETE-204";
        }
    }

}
