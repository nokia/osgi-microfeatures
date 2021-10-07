package com.nokia.as.microfeatures.bundlerepository;

import org.osgi.framework.VersionRange;
import org.osgi.resource.Requirement;

public interface RequirementBuilder {

	void addAttribute(final String key, final Object val);

	void addDirective(final String key, final String val);

	void addBundleIdentityFilter();

	void addBundleContentFilter();

	void addVersionRangeFilter(VersionRange versionRange);

	void multiOpFilter(char op, String... andFilter);

	String multiOp(char op, String... args);

	String op(char op, String l, String r);

	String eq(String l, String r);
	
	Requirement build();

}
