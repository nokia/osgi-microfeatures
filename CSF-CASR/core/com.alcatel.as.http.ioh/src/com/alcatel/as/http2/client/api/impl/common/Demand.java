package com.alcatel.as.http2.client.api.impl .common;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Encapsulates operations with demand (Reactive Streams).
 *
 * <p> Demand is the aggregated number of elements requested by a Subscriber
 * which is yet to be delivered (fulfilled) by the Publisher.
 */
public final class Demand {

    private final AtomicLong val = new AtomicLong();

    /**
     * Increases this demand by the specified positive value.
     *
     * @param n
     *         increment {@code > 0}
     *
     * @return {@code true} iff prior to this operation this demand was fulfilled
     */
    public boolean increase(long n) {
        if (n <= 0) {
            throw new IllegalArgumentException(String.valueOf(n));
        }
        long prev = val.getAndAccumulate(n, (p, i) -> p + i < 0 ? Long.MAX_VALUE : p + i);
        return prev == 0;
    }

    /**
     * Increases this demand by 1 but only if it is fulfilled.
     * @return true if the demand was increased, false otherwise.
     */
    public boolean increaseIfFulfilled() {
        return val.compareAndSet(0, 1);
    }

    /**
     * Tries to decrease this demand by the specified positive value.
     *
     * <p> The actual value this demand has been decreased by might be less than
     * {@code n}, including {@code 0} (no decrease at all).
     *
     * @param n
     *         decrement {@code > 0}
     *
     * @return a value {@code m} ({@code 0 <= m <= n}) this demand has been
     *         actually decreased by
     */
    public long decreaseAndGet(long n) {
        if (n <= 0) {
            throw new IllegalArgumentException(String.valueOf(n));
        }
        long p, d;
        do {
            d = val.get();
            p = Math.min(d, n);
        } while (!val.compareAndSet(d, d - p));
        return p;
    }

    /**
     * Tries to decrease this demand by {@code 1}.
     *
     * @return {@code true} iff this demand has been decreased by {@code 1}
     */
    public boolean tryDecrement() {
        return decreaseAndGet(1) == 1;
    }

    /**
     * @return {@code true} iff there is no unfulfilled demand
     */
    public boolean isFulfilled() {
        return val.get() == 0;
    }

    /**
     * Resets this object to its initial state.
     */
    public void reset() {
        val.set(0);
    }

    /**
     * Returns the current value of this demand.
     *
     * @return the current value of this demand
     */
    public long get() {
        return val.get();
    }

    @Override
    public String toString() {
        return String.valueOf(val.get());
    }
}
