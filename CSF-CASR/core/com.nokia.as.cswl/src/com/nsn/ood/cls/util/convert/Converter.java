/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.util.convert;

/**
 * @author marynows
 * 
 */
public interface Converter<T, U> {

	U convertTo(T value);

	T convertFrom(U value);
}
