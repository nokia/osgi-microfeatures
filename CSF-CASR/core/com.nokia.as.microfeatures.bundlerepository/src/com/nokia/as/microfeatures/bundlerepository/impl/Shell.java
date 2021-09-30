package com.nokia.as.microfeatures.bundlerepository.impl;

import static java.lang.System.out;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.ContentNamespace;

import com.nokia.as.microfeatures.bundlerepository.BundleRepository;
import com.nokia.as.microfeatures.bundlerepository.BundleRepository.InstallationResult;
import com.nokia.as.microfeatures.bundlerepository.RequirementBuilder;

//@Component(provides = Object.class)
//@Property(name = CommandProcessor.COMMAND_SCOPE, value = "microfeatures")
//@Property(name = CommandProcessor.COMMAND_FUNCTION, value = { 
//	"reload", "bundle", "capability", "resolveBundle", "resolveCapability", "installBundle", "installCapability" 
//})		
@Descriptor("Asr Bundle Repository Commands")
public class Shell {

	private final static Set<String> JAVA_ANNOTATION_PKG = new HashSet(Arrays.asList("javax.annotation", "javax.annotation.security", "javax.annotation.sql"));
	
	@ServiceDependency
	BundleRepository _repos;

	@Inject
	BundleContext _bc;
	
	@Descriptor("Reloads the OBR urls")
	public void reload() {
		try {
			_repos.reloadLocalObr();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Descriptor("Search for a bundle from all configured OBRs")
	public void bundle(
			@Descriptor("bundle version")
			@Parameter(names = "-v", absentValue="")
			String version,
			
			@Descriptor("display all informations (verbose)")
			@Parameter(names = "-a", absentValue="false", presentValue="true")
			boolean all,
			
			@Descriptor("bundle symbolic name (can contain some asterisk")
			String bsn)
	{
		try {
			RequirementBuilder requirementBuilder = _repos.newRequirementBuilder(IdentityNamespace.IDENTITY_NAMESPACE, bsn);
			if (version != null && version.length() > 0) {
				requirementBuilder.addVersionRangeFilter(new VersionRange(version));
			}
			requirementBuilder.addBundleIdentityFilter();
			List<Resource> resources = new ArrayList<Resource>();
			for (Capability c : _repos.findProviders(requirementBuilder.build())) {
				resources.add(c.getResource());
			}
			if (resources.isEmpty()) {
				out.println("No bundles found!");
			} else {
				printBundleResources(out, resources, all);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Descriptor("Search for the bundle matching a given capability filter")
	public void capability(
			@Descriptor("the capability name space (osgi.identity by default)")
			@Parameter(names = "-n", absentValue="osgi.identity")
			String namespace, 
						
			@Descriptor("display all informations (verbose)")
			@Parameter(names = "-a", absentValue="false", presentValue="true")
			boolean verbose,
			
			@Descriptor("the capability filter")
			String filter) 
	{
		try {
			RequirementBuilder requirementBuilder = _repos.newRequirementBuilder(namespace);
			if (namespace.equals("osgi.identity")) {
				requirementBuilder.addBundleIdentityFilter();
			}
			if (filter != null && filter.length() > 0) {
				try {
					FrameworkUtil.createFilter(filter);
				} catch (InvalidSyntaxException e) {
					out.println("Invalid filter: " + e.getMessage());
					return;
				}
				requirementBuilder.addDirective("filter", filter);
			}
			List<Capability> cs = _repos.findProviders(requirementBuilder.build());
			if (cs.isEmpty()) {
				out.println("No matching resources found!");
				return;
			}
			List<Resource> resources = new ArrayList<Resource>();
			for (Capability c : cs) {
				resources.add(c.getResource());
			}
			printResources(out, resources, "osgi.identity", verbose);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Descriptor("Resolve a bundle from the obr")
	public void resolveBundle(
			@Descriptor("the bundle version")
			@Parameter(names = "-v", absentValue="")
			String ver, 
			
			@Descriptor("Takes into account the locally installed bundles during the resolustion")
			@Parameter(names = "-l", absentValue="false", presentValue="true")
			boolean localResources, 
			
			@Descriptor("display all informations (verbose)")
			@Parameter(names = "-a", absentValue="false", presentValue="true")
			boolean all,
			
			@Descriptor("the bundle symbolic name")
			String bsn) 
	{
		try {
			RequirementBuilder requirementBuilder = _repos.newRequirementBuilder(IdentityNamespace.IDENTITY_NAMESPACE,
					bsn);
			if (ver != null && ver.length() > 0) {
				requirementBuilder.addVersionRangeFilter(new VersionRange(ver));
			}
			requirementBuilder.addBundleIdentityFilter();
			List<Capability> cs = _repos.findProviders(requirementBuilder.build());
			if (cs.isEmpty()) {
				out.println("No matching bundle found!");
				return;
			}

			try {
				Resource resource = cs.get(0).getResource();
				InstallationResult result = new InstallationResult();
				Set<Resource> resolvedResources = _repos.findResolution(Collections.singletonList(resource),
						localResources);
				result.resources.addAll(resolvedResources);
				printResources(out, resolvedResources, "osgi.identity", all);
			} catch (Exception e) {
				// out.println(e.getMessage());
				e.printStackTrace(out);
				return;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Descriptor("Resolve a given capability from the obr")
	public void resolveCapability(
			@Descriptor("the capability name space (osgi.identity by default)")
			@Parameter(names = "-n", absentValue="osgi.identity")
			String namespace, 
			
			@Descriptor("Takes into account the locally installed bundles during the resolustion")
			@Parameter(names = "-l", absentValue="false", presentValue="true")
			boolean localResources,
			
			@Descriptor("display all informations (verbose)")
			@Parameter(names = "-a", absentValue="false", presentValue="true")
			boolean all,
			
			@Descriptor("the capability filter")
			String filter)
	{
		try {
			RequirementBuilder requirementBuilder = _repos.newRequirementBuilder(namespace);
			try {
				FrameworkUtil.createFilter(filter);
			} catch (InvalidSyntaxException e) {
				out.println("Invalid filter: " + e.getMessage());
				return;
			}
			requirementBuilder.addDirective("filter", filter);

			List<Capability> capabilities = _repos.findProviders(requirementBuilder.build());
			if (capabilities.isEmpty()) {
				out.println("No matching bundle found!");
				return;
			}

			List<Resource> resourcesMatchingCapabilities = capabilities.stream().map(c -> c.getResource())
					.collect(Collectors.toList());

			InstallationResult result = new InstallationResult();
			Set<Resource> resolvedResources = _repos.findResolution(resourcesMatchingCapabilities, localResources);
			result.resources.addAll(resolvedResources);
			printResources(out, resolvedResources, "osgi.identity", all);
		} catch (Exception e) {
			// out.println(e.getMessage());
			e.printStackTrace(out);
			return;
		}
	}

	@Descriptor("Install a bundle and its dependencies from the obr")
	public void installBundle(
			@Descriptor("the bundle version")
			@Parameter(names = "-v", absentValue="")
			String ver, 
			
			@Descriptor("Start all installed bundles")
			@Parameter(names = "-s", absentValue="false", presentValue="true")
			boolean start,
			
			@Descriptor("the bundle symbolic name")
			String bsn)
	{
		try {
			RequirementBuilder requirementBuilder = _repos.newRequirementBuilder(IdentityNamespace.IDENTITY_NAMESPACE, bsn);
			if (ver != null && ver.length() > 0) {
				requirementBuilder.addVersionRangeFilter(new VersionRange(ver));
			}
			requirementBuilder.addBundleIdentityFilter();
			List<Capability> cs = _repos.findProviders(requirementBuilder.build());
			if (cs.isEmpty()) {
				out.println("No matching bundle found!");
				return;
			}

			Resource resource = cs.get(0).getResource();
			InstallationResult ir = _repos.install(Collections.singletonList(resource), true, start);

			for (String s : ir.userFeedback) {
				out.println(s);
			}
		} catch (final BundleException e) {
			Throwable t = e;
			while (t instanceof BundleException && ((BundleException) t).getNestedException() != null) {
				t = ((BundleException) t).getNestedException();
			}
			out.println("Couldn't install bundle (due to: " + t + ")");
			t.printStackTrace(out);
			return;

		} catch (Exception e) {
			// out.println(e.getMessage());
			e.printStackTrace(out);
			return;
		}
	}
	
	@Descriptor("Install a capability and its dependencies from the obr")
	public void installCapability(			
			@Descriptor("Start all installed bundles")
			@Parameter(names = "-s", absentValue="false", presentValue="true")
			boolean start,
			
			@Descriptor("the capability name space (osgi.identity by default)")
			@Parameter(names = "-n", absentValue="osgi.identity")
			String namespace, 
			
			@Descriptor("the capability filter")
			String filter)
	{
		try {
			RequirementBuilder requirementBuilder = _repos.newRequirementBuilder(namespace);
			try {
				FrameworkUtil.createFilter(filter);
			} catch (InvalidSyntaxException e) {
				out.println("Invalid filter: " + e.getMessage());
				return;
			}
			requirementBuilder.addDirective("filter", filter);

			List<Capability> capabilities = _repos.findProviders(requirementBuilder.build());
			if (capabilities.isEmpty()) {
				out.println("No matching bundle found!");
				return;
			}

			List<Resource> resourcesMatchingCapabilities = capabilities.stream().map(c -> c.getResource())
					.collect(Collectors.toList());
			
			InstallationResult ir = _repos.install(resourcesMatchingCapabilities, true, start);

			for (String s : ir.userFeedback) {
				out.println(s);
			}

		} catch (Exception e) {
			// out.println(e.getMessage());
			e.printStackTrace(out);
			return;
		}
	}
	

	private void printBundleResources(PrintStream out, List<Resource> resources, boolean verbose) {
		out.println("I Bundle resource");
		out.println("- --------------------");
		for (Resource r : resources) {
			Map<String, Object> identity = r.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).iterator().next()
					.getAttributes();
			out.print(isInstalled(r) ? "* " : "  ");
			out.print(identity.get(IdentityNamespace.IDENTITY_NAMESPACE));
			out.print(", version=");
			out.println(identity.get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE));
			if (verbose) {
				Map<String, Object> content = r.getCapabilities(ContentNamespace.CONTENT_NAMESPACE).iterator().next()
						.getAttributes();
				out.println("    Type: " + identity.get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE));
				out.println("    URL:  " + content.get(ContentNamespace.CAPABILITY_URL_ATTRIBUTE));
				out.println("    Size: " + content.get(ContentNamespace.CAPABILITY_SIZE_ATTRIBUTE));
			}
		}
	}

	private boolean isInstalled(Resource r) {
		Map<String, Object> identity = r.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).iterator().next()
				.getAttributes();
		String name = (String) identity.get(IdentityNamespace.IDENTITY_NAMESPACE);
		Version version = (Version) identity.get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
		Map<String, Object> content = r.getCapabilities(ContentNamespace.CONTENT_NAMESPACE).iterator().next()
				.getAttributes();
		Bundle lb = _bc.getBundle((String) content.get(ContentNamespace.CAPABILITY_URL_ATTRIBUTE));
		if (lb != null && name.equals(lb.getSymbolicName()) && version.equals(lb.getVersion())) {
			return true;
		}
		for (Bundle b : _bc.getBundles()) {
			if (name.equals(b.getSymbolicName()) && version.equals(b.getVersion())) {
				return true;
			}
		}
		return false;
	}

	private void printResources(PrintStream out, Collection<Resource> resources, String namespace, boolean verbose) {
		out.println("Resolution result:");
		for (Resource r : resources) {
			final List<Capability> capabilities = r.getCapabilities(namespace);
			Map<String, Object> nsAttrs = capabilities.iterator().next().getAttributes();
			out.println(nsAttrs.get(namespace) + ":" + nsAttrs.get("version"));

			if (verbose) {
				out.println("---------------");
				out.println("  Capabilities:");
				final List<Capability> caps = new ArrayList<Capability>(r.getCapabilities(null));
				Collections.sort(caps, new Comparator<Capability>() {
					@SuppressWarnings("unchecked")
					@Override
					public int compare(Capability c1, Capability c2) {
						final String ns = c1.getNamespace();
						int res = ns.compareTo(c2.getNamespace());
						if (res == 0) {
							Object a1 = c1.getAttributes().get(ns);
							if (a1 instanceof Comparable) {
								res = ((Comparable<Object>) a1).compareTo(c2.getAttributes().get(ns));
							}
						}
						return res;
					}
				});
				String oldNs = null;
				for (Capability rc : caps) {
					String ns = rc.getNamespace();
					if (!ns.equals(oldNs)) {
						out.println("    Namespace: " + ns);
						oldNs = ns;
					} else {
						out.println("     --");
					}
					printMap(out, rc.getAttributes(), "       ", " = ");
					printMap(out, rc.getDirectives(), "       ", " := ");
				}
				out.println("  Requirements:");
				final List<Requirement> reqs = new ArrayList<Requirement>(r.getRequirements(null));
				Collections.sort(reqs, new Comparator<Requirement>() {
					@SuppressWarnings("unchecked")
					@Override
					public int compare(Requirement r1, Requirement r2) {
						final String ns = r1.getNamespace();
						int res = ns.compareTo(r2.getNamespace());
						if (res == 0) {
							Object a1 = r1.getAttributes().get(ns);
							if (a1 instanceof Comparable) {
								res = ((Comparable<Object>) a1).compareTo(r2.getAttributes().get(ns));
							}
						}
						return res;
					}
				});
				oldNs = null;
				for (Requirement rr : r.getRequirements(null)) {
					String ns = rr.getNamespace();
					if (!ns.equals(oldNs)) {
						out.println("    Namespace: " + ns);
						oldNs = ns;
					} else {
						out.println("     --");
					}
					printMap(out, rr.getAttributes(), "       ", " = ");
					printMap(out, rr.getDirectives(), "       ", " := ");
				}
				out.println();
			}
		}
	}

	private void printMap(PrintStream out, Map<String, ?> m, String prefix, String div) {
		for (Entry<String, ?> e : m.entrySet()) {
			out.print(prefix);
			out.print(e.getKey());
			out.print(div);
			out.println(e.getValue());
		}
	}

}
