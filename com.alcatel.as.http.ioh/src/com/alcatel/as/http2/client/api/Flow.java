package com.alcatel.as.http2.client.api;

public final class Flow {

    private Flow() {}

    @FunctionalInterface
    public static interface Publisher<T> {
        public void subscribe(Subscriber<? super T> subscriber);
    }

    public static interface Subscriber<T> {
        public void onSubscribe(Subscription subscription);

        public void onNext(T item);

        public void onError(Throwable throwable);

        public void onComplete();
    }

    public static interface Subscription {
        public void request(long n);

        public void cancel();
    }

}
