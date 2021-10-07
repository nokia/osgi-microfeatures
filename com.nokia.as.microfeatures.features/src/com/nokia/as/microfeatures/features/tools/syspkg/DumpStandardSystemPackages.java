package com.nokia.as.microfeatures.features.tools.syspkg;

import java.util.List;

import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.NameValuePair;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.Start;
import org.osgi.framework.BundleContext;

/**
 * Tool used to display list of standard system packages that are reexported by system bundle.
 */
@Component
public class DumpStandardSystemPackages {

	@Inject
	BundleContext _bc;
	
	@Start
	void start() {
		String pkgsExportedBySystemBundle = _bc.getBundle(0).getHeaders().get("Export-Package");
		List<NameValuePair> pairs = ManifestHeaderProcessor.parseExportString(pkgsExportedBySystemBundle);
		pairs.sort((pair1, pair2) -> pair1.getName().compareTo(pair2.getName()));
		System.out.println();
		System.out.println("  java.*,\\");
		pairs.stream()
		     .filter((p) -> ! p.getName().startsWith("java."))
		     .forEach(p -> System.out.println("  " + p.getName() + "\\,"));					
	}
}
