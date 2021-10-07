package com.nokia.as.h2client.test.client;

import com.alcatel.as.http2.client.api.HttpClientFactory;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public interface TestSuit {
    default CompletableFuture<Boolean> run(HttpClientFactory factory) {

        List<CompletableFuture<Boolean>>
                all_cf = tests().stream().map(test -> {
                    Http2ClientTestRunner runner;
                    runner = new Http2ClientTestRunner() {
                    };
                    Context context = new Context(factory, test);
                    runner.run(context);
                    return runner.cf;
                }
        ).collect(Collectors.toList());

        CompletableFuture<Boolean> [] array = all_cf.toArray(new CompletableFuture[all_cf.size()]);

        CompletableFuture<Void> join = CompletableFuture.allOf(array);
        CompletableFuture<Boolean> aggregate = join.thenApply( result ->
                 all_cf
                        .stream()
                        .map(a -> a.getNow(false))
                        .reduce(true, (a, b) -> a && b)

        );
        return aggregate;
    }

    List<Http2ClientTestDescriptor> tests() ;
}
