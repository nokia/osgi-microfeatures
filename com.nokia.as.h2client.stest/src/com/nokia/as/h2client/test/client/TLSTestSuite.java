package com.nokia.as.h2client.test.client;

import com.alcatel.as.http2.client.api.HttpClient;
import com.alcatel.as.http2.client.api.HttpRequest;
import com.alcatel.as.http2.client.api.HttpResponse;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TLSTestSuite implements TestSuit {

    @Override
    public List<Http2ClientTestDescriptor> tests() {
        return Collections.unmodifiableList(
                Arrays.asList(new Http2ClientTestDescriptor[]{
                        new TLS_12_ok()
                        , new TLS_12_nok_encryption()
                })
        );
    }

    abstract class TLSParentTestDescriptor implements Http2ClientTestDescriptor<String> {
        @Override
        public HttpClient.Builder buildHttpClient(Context context, HttpClient.Builder builder) {
            return builder
                    .secureProtocols(Collections.singletonList("TLSv1.2"))
                    .secureCipher(Collections.singletonList("TLS_RSA_WITH_AES_128_CBC_SHA"))
//                .secureCipher(Collections.singleton("DHE-RSA-AES256-GCM-SHA384"))
//                    .secureKeystoreFile("/usr/local/etc/nginx/cacerts") // -> Handshake failure avec nginx:  ssl_ciphers EECDH+AESGCM:EDH+AESGCM:AES256+EECDH:AES256+EDH;
                    .secureKeystoreFile("/tmp/client.pkcs12")
                    .secureKeystorePwd("password")
                    ;
        }

        @Override
        public HttpRequest.Builder buildRequest(Context context, HttpRequest.Builder builder) {
            return builder
                    .uri(URI.create("https://127.0.0.1:9093/services/test/generator"))
                    ;
        }

        @Override
        public HttpResponse.BodyHandler<String> bodyHandler(Context context) {
            return context.getFactory().bodyHandlers().ofString();
        }

    }

    class TLS_12_ok extends TLSParentTestDescriptor {

        @Override
        public HttpRequest.Builder buildRequest(Context context, HttpRequest.Builder builder) {
            return super.buildRequest(context, builder)
                    .GET()
                    ;
        }

        @Override
        public String name() {
            return "TLS-12-ok";
        }
    }

    class TLS_12_nok_encryption extends TLSParentTestDescriptor {
        // this is the root exception
        //    but unfortunately it is hidden
        //
        //2019-03-08 14:42:17,029 INFO as.service.reactor.Main.TcpChannelConnector  Selector-1 - Could not connect to /127.0.0.1:9093
        //
        //java.lang.IllegalArgumentException: Unsupported ciphersuite DHE-RSA-AES256-GCM-SHA384
        //        at sun.security.ssl.CipherSuite.valueOf(CipherSuite.java:282)
        //        at sun.security.ssl.CipherSuiteList.<init>(CipherSuiteList.java:79)
        //        at sun.security.ssl.SSLEngineImpl.setEnabledCipherSuites(SSLEngineImpl.java:2141)
        //        at alcatel.tess.hometop.gateways.reactor.impl.TLSEngineImpl.<init>(TLSEngineImpl.java:149)
        //        at alcatel.tess.hometop.gateways.reactor.impl.TcpChannelSecureImpl.<init>(TcpChannelSecureImpl.java:55)
        //        at alcatel.tess.hometop.gateways.reactor.impl.TcpChannelConnector.createChannel(TcpChannelConnector.java:129)
        //        at alcatel.tess.hometop.gateways.reactor.impl.TcpChannelConnector.connected(TcpChannelConnector.java:200)
        //        at alcatel.tess.hometop.gateways.reactor.impl.TcpChannelConnector.selected(TcpChannelConnector.java:110)
        //        at alcatel.tess.hometop.gateways.reactor.impl.NioSelector.loopOnSelectedKeys(NioSelector.java:365)
        //        at alcatel.tess.hometop.gateways.reactor.impl.NioSelector.run(NioSelector.java:136)
        //        at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
        //        at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
        //        at java.lang.Thread.run(Thread.java:748)

        @Override
        public CompletableFuture<Boolean> processResult(Context context, CompletableFuture<HttpResponse<String>> cf) {

            CompletableFuture<Boolean> cfr = new CompletableFuture<>();
            cf.handleAsync((response, exception) -> {
                if (exception != null && exception instanceof java.net.PortUnreachableException) {
                        cfr.complete(Boolean.TRUE);
                } else
                    cfr.complete(Boolean.FALSE);
                return response;
            });
            return cfr;
        }

        @Override
        public HttpClient.Builder buildHttpClient(Context context, HttpClient.Builder builder) {
            return builder
                    .secureProtocols(Collections.singletonList("TLSv1.2"))
                    .secureCipher(Collections.singletonList("DHE-RSA-AES256-GCM-SHA384"))
                    .secureKeystoreFile("/tmp/client.pkcs12")
                    .secureKeystorePwd("password")
                    ;
        }

        @Override
        public HttpRequest.Builder buildRequest(Context context, HttpRequest.Builder builder) {
            return super.buildRequest(context, builder)
                    .GET()
                    ;
        }

        @Override
        public String name() {
            return "TLS_12_nok_encryption";
        }
    }


}
