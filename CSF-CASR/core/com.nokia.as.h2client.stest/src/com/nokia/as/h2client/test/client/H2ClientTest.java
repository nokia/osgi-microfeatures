package com.nokia.as.h2client.test.client;

import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.alcatel.as.http2.client.api.HttpClient;
import com.alcatel.as.http2.client.api.HttpClientFactory;
import com.alcatel.as.http2.client.api.HttpRequest;
import com.alcatel.as.http2.client.api.HttpResponse;
import com.nokia.as.util.junit4osgi.OsgiJunitRunner;

@Component(provides = Object.class)
@Property(name = OsgiJunitRunner.JUNIT, value = "true")
@RunWith(OsgiJunitRunner.class)
public class H2ClientTest {
    private static Logger _logger = Logger.getLogger("TestService");
    private HttpClientFactory _factory;

    @ServiceDependency
    public void set(HttpClientFactory factory) {
        _logger.warn("@Reference: HttpClientFactory:" + factory.toString());
        _factory = factory;
    }

	@Before
	public void init() {
	}

	@Test
	public void testH2Client() {
        _logger.warn("activating service");

        Http2ClientTestDescriptor.Context context = new Http2ClientTestDescriptor.Context() {

            Http2ClientTestDescriptor _test = test6;

            Logger _logger = Logger.getLogger(_test.name());

            @Override
            public Logger getLogger() {
                return _logger;
            }

            @Override
            public HttpClientFactory getFactory() {
                return _factory;
            }

            @Override
            public Http2ClientTestDescriptor getTest() {
                return _test;
            }
        };

        try {
            boolean result1 = new JaxRsTestSuite().run(_factory).get(10, TimeUnit.SECONDS);
            boolean result2 = new TLSTestSuite().run(_factory).get(10, TimeUnit.SECONDS);
            boolean result = result1 && result2;
            if (! result) {
                Assert.fail();
            }
//            if (result)
//                System.exit(0);
//            else {
//                System.exit(-1);
//            }
        } catch (InterruptedException e) {    		
            e.printStackTrace();
            Assert.fail();
        } catch (ExecutionException e) {
            e.printStackTrace();
            Assert.fail();
        } catch (TimeoutException e) {
            e.printStackTrace();
            Assert.fail();
        }
//        new Http2ClientTestRunner(){}.run(context);
    }

    Http2ClientTestDescriptor test5 = new Http2ClientTestDescriptor<String>() {
        @Override
        public HttpClient.Builder buildHttpClient(Context context, HttpClient.Builder builder) {
            return builder
            ;
        }

        @Override
        public HttpRequest.Builder buildRequest(Context context, HttpRequest.Builder builder) {
            return builder
                    .uri(URI.create("http://127.0.0.1:9092/services/hello?size=100&update"))
                    .header("Content-Type", "application/json")
                    .GET()
                    ;
        }

        @Override
        public HttpResponse.BodyHandler<String> bodyHandler(Context context) {
            return context.getFactory().bodyHandlers().ofString();
        }

        @Override
        public boolean checkContent(Context context, String body) {
            return true;
        }

        @Override
        public String name() {
            return "Test-5";
        }
    };

    Http2ClientTestDescriptor test6 = new Http2ClientTestDescriptor<String>() {
        @Override
        public HttpClient.Builder buildHttpClient(Context context, HttpClient.Builder builder) {
            return builder
            ;
        }

        String content = "{  \"ipv4Addr\":\"10.182.119.20\", \"ipv6Prefix\": \"2001:0db8:85a3:0000:0000:8a2e:0370:7334\", \"macAddr48\": \"00-14-22-01-23-45\", " +
                "\"supi\": \"284011234567890\", \"gpsi\": \"9876543210\", \"ipDomain\": \"ipdomains\", \"dnn\": \"internet.mnc012.mcc345.gprs\", \"pcfFqdn\": \"nsn.com\", \"pcfIpEndPoints\": [{" +
                " \"ipv4Address\": \"10.0.0.100\", \"ipv6Address\": \"2001:0db8:85a3:0000:0000:8a2e:0370:7334\", \"transport\": \"TCP\", \"port\": 9999 }], \"pcfDiamHost\": \"nsn.example.com\"," +
                " \"pcfDiamRealm\": \"nsn.realm.com\", \"snssai\": { \"sst\": 1, \"sd\": \"19CDE0\" }}";

        @Override
        public HttpRequest.Builder buildRequest(Context context, HttpRequest.Builder builder) {
            InputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

            return builder
                    .uri(URI.create("http://127.0.0.1:9092/services/hello?size=100&update"))
                    .header("some-bad-Header", "application/json")
                    .header("some-plain-header", "application/json")
                    .header("content-type", "application/json")
                    .POST(_factory.bodyPublishers().ofInputStream(() -> inputStream))
                    ;
        }

        @Override
        public HttpResponse.BodyHandler<String> bodyHandler(Context context) {
            return context.getFactory().bodyHandlers().ofString();
        }

        @Override
        public boolean checkContent(Context context, String body) {
            String bodyAsString = (String)body;
            return bodyAsString.contains(content)
                    ;
        }

        @Override
        public String name() {
            return "Test-6";
        }
    };
}
