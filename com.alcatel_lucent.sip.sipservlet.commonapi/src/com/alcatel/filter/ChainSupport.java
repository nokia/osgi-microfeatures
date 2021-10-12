// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.filter;

import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;

/**  Description of the Interface */
public interface ChainSupport {
	final byte IR = 0;
	final byte OR = 1;
	final byte IS = 2;
	final byte OS = 3;


	/**
	 *  Sets the filterChain attribute of the ChainSupport object
	 *
	 *@param  chain  The new filterChain value
	 */
	void setFilterChain(FilterChain[] chain);


	/**
	 *  Gets the filterChain attribute of the ChainSupport object
	 *
	 *@return    The filterChain value
	 */
	FilterChain[] getFilterChains();


	/**
	 *  Sets the chained attribute of the ChainSupport object
	 *
	 *@param  b  The new chained value
	 */
    void setFilters(List<? extends Filter> list);
}

