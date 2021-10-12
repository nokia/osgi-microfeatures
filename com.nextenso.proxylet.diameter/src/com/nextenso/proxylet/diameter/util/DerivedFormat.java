// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.diameter.util;

import java.util.Objects;

public class DerivedFormat extends DiameterAVPFormat {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String name;
	private DiameterAVPFormat baseFormat;
	
	public DerivedFormat(String name, DiameterAVPFormat baseFormat) {
		Objects.requireNonNull(name);
		Objects.requireNonNull(baseFormat);
		this.name = name;
		this.baseFormat = baseFormat;
	}
	
	@Override
	public String toString(byte[] data, int off, int len, int level) {
		return baseFormat.toString(data, off, len, level);
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	public DiameterAVPFormat getBaseFormat() {
		return baseFormat;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof DerivedFormat)) {
			return false;
		}
		
		DerivedFormat other = (DerivedFormat) obj;
		return Objects.equals(name, other.name) &&
				Objects.equals(baseFormat, other.baseFormat);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(name, baseFormat);
	}

	@Override
	public byte[] encode(Object value) throws IllegalArgumentException {
		return baseFormat.encode(value);
	}
}
