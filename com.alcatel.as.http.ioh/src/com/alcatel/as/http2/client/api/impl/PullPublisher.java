// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.http2.client.api.impl;

import java.util.Iterator;
import com.alcatel.as.http2.client.api.Flow;
import com.alcatel.as.http2.client.api.impl.common.Demand;
import com.alcatel.as.http2.client.api.impl.common.SequentialScheduler;

/**
 * A Publisher that publishes items obtained from the given Iterable. Each new
 * subscription gets a new Iterator.
 */
class PullPublisher<T> implements Flow.Publisher<T> {

    // Only one of `iterable` and `throwable` can be non-null. throwable is
    // non-null when an error has been encountered, by the creator of
    // PullPublisher, while subscribing the subscriber, but before subscribe has
    // completed.
    private final Iterable<T> iterable;
    private final Throwable throwable;

    PullPublisher(Iterable<T> iterable, Throwable throwable) {
        this.iterable = iterable;
        this.throwable = throwable;
    }

    PullPublisher(Iterable<T> iterable) {
        this(iterable, null);
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        Subscription sub;
        if (throwable != null) {
            assert iterable == null : "non-null iterable: " + iterable;
            sub = new Subscription(subscriber, null, throwable);
        } else {
            assert throwable == null : "non-null exception: " + throwable;
            sub = new Subscription(subscriber, iterable.iterator(), null);
        }
        subscriber.onSubscribe(sub);

        if (throwable != null) {
            sub.pullScheduler.runOrSchedule();
        }
    }

    private class Subscription implements Flow.Subscription {

        private final Flow.Subscriber<? super T> subscriber;
        private final Iterator<T> iter;
        private volatile boolean completed;
        private volatile boolean cancelled;
        private volatile Throwable error;
        final SequentialScheduler pullScheduler = new SequentialScheduler(new PullTask());
        private final Demand demand = new Demand();

        Subscription(Flow.Subscriber<? super T> subscriber,
                     Iterator<T> iter,
                     Throwable throwable) {
            this.subscriber = subscriber;
            this.iter = iter;
            this.error = throwable;
        }

        final class PullTask extends SequentialScheduler.CompleteRestartableTask {
            @Override
            protected void run() {
                if (completed || cancelled) {
                    return;
                }

                Throwable t = error;
                if (t != null) {
                    completed = true;
                    pullScheduler.stop();
                    subscriber.onError(t);
                    return;
                }

                while (demand.tryDecrement() && !cancelled) {
                    if (!iter.hasNext()) {
                        break;
                    } else {
                        subscriber.onNext(iter.next());
                    }
                }
                if (!iter.hasNext() && !cancelled) {
                    completed = true;
                    pullScheduler.stop();
                    subscriber.onComplete();
                }
            }
        }

        @Override
        public void request(long n) {
            if (cancelled)
                return;  // no-op

            if (n <= 0) {
                error = new IllegalArgumentException("illegal non-positive request:" + n);
            } else {
                demand.increase(n);
            }
            pullScheduler.runOrSchedule();
        }

        @Override
        public void cancel() {
            cancelled = true;
        }
    }
}
