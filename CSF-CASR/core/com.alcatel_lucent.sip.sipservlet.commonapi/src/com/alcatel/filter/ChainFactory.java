package com.alcatel.filter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;

import java.util.List;
import java.util.Map;

public interface ChainFactory {
	    // this leads to to many issues ... (compiler bug etc...)
        // FilterChain create(Map<String,? extends ServletLoader > servletmap,String declaration,List<Filter> filters);
        FilterChain create(String chainname,Map<String,?> servletmap,String declaration,List<Filter> filters,List<String> bannedlist,String appname);
        FilterChain create(String chainname,Filter... filters);

}
