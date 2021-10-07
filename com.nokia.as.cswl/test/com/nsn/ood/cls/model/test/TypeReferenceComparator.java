/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.test;

import java.util.Comparator;

import com.fasterxml.jackson.core.type.TypeReference;


/**
 * @author marynows
 * 
 */
public class TypeReferenceComparator implements Comparator<TypeReference<?>> {

	@Override
	public int compare(final TypeReference<?> ref1, final TypeReference<?> ref2) {
		if (ref1.getType().equals(ref2.getType())) {
			return 0;
		}
		return 1;
	}
}