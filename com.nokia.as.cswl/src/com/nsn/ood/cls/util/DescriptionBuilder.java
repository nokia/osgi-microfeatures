/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.util;

/**
 * @author marynows
 * 
 */
public class DescriptionBuilder {
	private final StringBuilder sb = new StringBuilder();

	public String build() {
		return this.sb.insert(0, "[").append("]").toString();
	}

	public DescriptionBuilder append(final String name, final Object value) {
		if (value != null) {
			if (this.sb.length() > 0) {
				this.sb.append(", ");
			}
			this.sb.append(name).append("=").append(value);
		}
		return this;
	}
}
