package com.nokia.as.h2client.test.client;

import com.alcatel.as.http2.client.api.HttpClient;
import com.alcatel.as.http2.client.api.HttpClientFactory;
import com.alcatel.as.http2.client.api.HttpRequest;
import com.alcatel.as.http2.client.api.HttpResponse;
import org.apache.log4j.Logger;

import java.util.concurrent.CompletableFuture;

public interface Http2ClientTestDescriptor<T> {

    default HttpClient.Builder buildHttpClient(Context context, HttpClient.Builder builder) {
        return builder;
    }

    HttpRequest.Builder buildRequest(Context context, HttpRequest.Builder builder);

    HttpResponse.BodyHandler<T> bodyHandler(Context context);

    default CompletableFuture<Boolean> processResult(Context context, CompletableFuture<HttpResponse<T>> cf) {
        CompletableFuture<Boolean> cfr = new CompletableFuture<>();
        cf.handleAsync((response, exception) -> {
            if (exception != null) {
                context.getLogger().error("failed:" + exception,exception);
                cfr.completeExceptionally(exception);
            } else {
                context.getLogger().warn("status code:" + response.statusCode());
                if (!checkStatusCode(context, response.statusCode())) {
                    context.getLogger().error("status code is not acceptable:" + response.statusCode());
                    cfr.complete(Boolean.FALSE);
                } else {
                    if ( ! checkContent(context, response.body())) {
                        context.getLogger().error("body was not accepted:" + response.body());
                        cfr.complete(Boolean.FALSE);
                    } else {
                        context.getLogger().debug("body is ok:" + response.body());
                        cfr.complete(Boolean.TRUE);
                    }
                }
            }
            return response;
        });
        return cfr;
    }

    default boolean checkStatusCode(Context context, int statusCode) {
        return (statusCode >= 200 && statusCode < 300);
    }

    default boolean checkContent(Context context, T body) {
        return true;
    }

    default String name() {
        return this.getClass().getName();
    }

    interface Context {
        Logger getLogger();
        HttpClientFactory getFactory();
        Http2ClientTestDescriptor getTest();
    }
}

