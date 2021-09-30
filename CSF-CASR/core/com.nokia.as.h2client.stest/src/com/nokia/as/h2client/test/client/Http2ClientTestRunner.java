package com.nokia.as.h2client.test.client;

import com.alcatel.as.http2.client.api.HttpClient;
import com.alcatel.as.http2.client.api.HttpRequest;
import com.alcatel.as.http2.client.api.HttpResponse;

import java.util.concurrent.CompletableFuture;

public interface Http2ClientTestRunner {

    CompletableFuture<Boolean> cf = new CompletableFuture<>();

    default <T> void run(Http2ClientTestDescriptor.Context context) {

        Http2ClientTestDescriptor test = context.getTest();
        HttpClient  _client;
        HttpRequest _request;

        // 1. build http-client
        _client = test.buildHttpClient(
                context
                , context.getFactory().newHttpClientBuilder()
                        .setProperty("logger",context.getLogger())
        )
                .build();

        _request = test.buildRequest( context, context.getFactory().newHttpRequestBuilder()).build();

        CompletableFuture<HttpResponse<T>> cf = _client.sendAsync(_request, test.bodyHandler(context));

        CompletableFuture<Boolean> cf2 = test.processResult(context, cf);

        cf2.handle( (result, exception) -> {
            if (exception != null) {
                context.getLogger().debug("CompletableFuture caught unexpected exception", exception);
                completion(context, false);
            } else if (result == null) {
                context.getLogger().debug("CompletableFuture's result is null");
                completion(context, false);
            } else {
                completion(context, result);
            }
            return null;
        } );

        cf2.handle( (x,e) -> {
            context.getLogger().warn("invoking close");
            _client.close() ;
            return null;
        } );

    }

    default void completion(Http2ClientTestDescriptor.Context context, Boolean result) {
        if (! result) {
            context.getLogger().error(context.getTest().toString() + " FAILED");
            cf.complete(false);
        } else {
            context.getLogger().warn(context.getTest().toString() + " SUCCEED");
            cf.complete(true);
        }
    }
}
