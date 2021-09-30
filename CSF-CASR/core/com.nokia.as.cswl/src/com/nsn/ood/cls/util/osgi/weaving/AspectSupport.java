package com.nsn.ood.cls.util.osgi.weaving;

import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aspectj.weaver.loadtime.definition.Definition;
import org.aspectj.weaver.loadtime.definition.DocumentParser;

public interface AspectSupport {

	static List<Definition> definitionList(ClassLoader loader, String resource) {
		Map<URL, Definition> definitionMap = new HashMap<>();
		try {
			Enumeration<URL> resourcezList = loader.getResources(resource);
			while (resourcezList.hasMoreElements()) {
				URL url = resourcezList.nextElement();
				Definition definition = DocumentParser.parse(url);
				definitionMap.put(url, definition);
			}
			return new ArrayList<>(definitionMap.values());
		} catch (Exception e) {
			throw new Error(e);
		}
	}

}
