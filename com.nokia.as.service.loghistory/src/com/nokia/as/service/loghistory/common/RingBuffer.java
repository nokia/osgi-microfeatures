// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.service.loghistory.common;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public class RingBuffer<T> extends ConcurrentLinkedQueue<T> {

	private static final long serialVersionUID = -6413104704460950159L;
	private final int capacity;
	private AtomicLong size = new AtomicLong(0);
	
	public RingBuffer() {
		this(1000);
	}

	public RingBuffer(int capacity) {
		this.capacity = capacity;
	}

	public void put(T e) {
		if(size.incrementAndGet() > capacity) {
			super.poll();
		}
		super.add(e);
	}
	
	public void put(List<T> items){
		items.forEach(c -> {
				put(c);
		});
	}
}
