// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.microfeatures.bundlerepository.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wiring;
import org.osgi.service.repository.Repository;
import org.osgi.service.resolver.HostedCapability;
import org.osgi.service.resolver.ResolveContext;

import aQute.bnd.repository.osgi.OSGiRepository;

public class ResolveContextImpl extends ResolveContext {
	private final BundleContext _bc;
	private final List<Resource> _mandatory;
	private final List<Resource> _optional;
	private Map<Resource, Wiring> _wirings = null;
    private final Map<URI, OSGiRepository> _repositories;
	private final boolean _localResources;
    private final CandidateComparator _candidateComparator;
	private final Set<Resource> _blacklist;
	private final Map<Requirement, List<Capability>> m_capabilities = new HashMap<>();
	private final boolean _resolveOptionalDependencies;
	private final Map<Resource, Wiring> _localWirings;
	private final boolean _hasCSFAR_1822Bug;
	private final static String AGENT_LB_LEVEL4_BSN = "com.alcatel.as.ioh.impl.agent-lb.feature";
	
	public ResolveContextImpl(Map<URI, OSGiRepository> repositories, BundleContext bc, List<Resource> mandatory, List<Resource> optional, boolean localresources, Set<Resource> blacklist, 
							  boolean resolveOptionalDependencies, boolean hasCSFAR_1822Bug) 
	{
		_repositories = repositories;
		_bc = bc;
		_mandatory = mandatory;
		_optional = optional;
		_localResources = localresources;
		_candidateComparator = new CandidateComparator(new HashSet<>(_mandatory));
		_blacklist = blacklist;
		_resolveOptionalDependencies = resolveOptionalDependencies;
		_localWirings = getLocalFrameworkWirings();
		_hasCSFAR_1822Bug = hasCSFAR_1822Bug;
	}

	@Override
	public Collection<Resource> getMandatoryResources() {
		return _mandatory;
	}

	@Override
	public Collection<Resource> getOptionalResources() {
		return _optional;
	}
	
	@Override
	public synchronized List<Capability>  findProviders(Requirement r) {
		List<Capability> cachedCapabilities = m_capabilities.get(r);		
		if (cachedCapabilities != null) {
			return cachedCapabilities;
		}
		
		List<Capability> capabilities = new ArrayList<Capability>();
		
		// See if the felix framework provides the required capabilities.
		addToProvidersIfMatching(_bc.getBundle(0), capabilities, r);

		// optionally see if local resources provide the required capabilities.
		if (_localResources) {
			Stream.of(_bc.getBundles()).filter(b -> b.getBundleId() != 0).forEach(b -> addToProvidersIfMatching(b, capabilities, r));
		}

        // Find from repositories
        for (Entry<URI, OSGiRepository> repoEntry : _repositories.entrySet()) {
            Repository repository = repoEntry.getValue();
            Map<Requirement, Collection<Capability>> providers = repository.findProviders(Collections.singleton(r));
            if (providers != null) {
                Collection<Capability> repoCaps = providers.get(r);
                if (repoCaps != null) {
                	repoCaps.stream()
						.filter(cap -> ! _blacklist.contains(cap.getResource()))
						.forEach(capabilities::add);
                }
            }
        }        
		
		// Sort caps
        Collections.sort(capabilities, _candidateComparator);

        m_capabilities.put(r, capabilities);
		return capabilities;
	}
	
	private void addToProvidersIfMatching(Bundle b, List<Capability> providers, Requirement r) {
		BundleRevision br = b.adapt(BundleRevision.class);
        addToProvidersIfMatching(br, providers, r);
	}
	
	private void addToProvidersIfMatching(Resource res, List<Capability> providers, Requirement req) {
		String f = req.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
		Filter filter = null;
		if(f != null) {
		  try {
		    filter = _bc.createFilter(f);
		  } catch (InvalidSyntaxException e) {
		    // TODO log filter failure, skip
		    System.err.println("Failed, " + f + ". " + e);
		    return;
		  }
		}
		for(Capability c : res.getCapabilities(req.getNamespace())) {
		  if(filter != null && !filter.matches(c.getAttributes())) {
		    continue;
		  }
		  providers.add(c);
		}
	}

	@Override
	public int insertHostedCapability(List<Capability> capabilities, HostedCapability hostedCapability) {
        int idx = Collections.binarySearch(capabilities, hostedCapability, _candidateComparator);
        if (idx < 0) {
            idx = Math.abs(idx + 1);
        }
        capabilities.add(idx, hostedCapability);
        return idx;
	}
	
	@Override
	public boolean isEffective(Requirement requirement) {
		if (requirement.getNamespace().equals("osgi.ee")) {
			return false;
		}
		
		// Ignore an old bugged "com.alcatel.as.felix.asmb" feature declaring an invalid "Require-Bundle: org.apache.felix.main" header.
		if (requirement.getNamespace().equals("osgi.wiring.bundle")) {
			String filter = requirement.getDirectives().get("filter");
			if (filter.equals("(symbolicname=org.apache.felix.main)")) {
				return false;
			}					
		}
		
		if (requirement.getNamespace().equals("osgi.wiring.package")) {
			String filter = requirement.getDirectives().get("filter");

			// Ignore a wrong requirement that was present in an old dependency manager
			if (filter != null && filter.equals("(&(osgi.wiring.package=org.osgi.framework)(version>=1.8.0)(!(version>=1.9.0)))")) {
				return false;
			}
		}
		
		// Ignore requirement on system bundle. the jre system bundles has a requirement on the system bundle which is always satisfied
		if (requirement.getNamespace().equals("osgi.wiring.host")) {
			String filter = requirement.getDirectives().get("filter");
			if (filter != null && filter.startsWith("(&(osgi.wiring.host=system.bundle)(bundle-version")) {
				return false;
			}
		}
				
		// Ignore a Require-Capability on osgi.identity=org.apache.felix.main
		if (requirement.getNamespace().equals("osgi.identity")) {
			String filter = requirement.getDirectives().get("filter");
			if (filter.equals("(osgi.identity=org.apache.felix.main)")) {
				return false;
			}
		}
		
		// Ignore optional requirement (except if we must resolve optional dependencies)
		String resolution = requirement.getDirectives().get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE);
		if (! _resolveOptionalDependencies && Namespace.RESOLUTION_OPTIONAL.equals(resolution)) {
			return false;
		}	
		
		if (_hasCSFAR_1822Bug) {
			Resource r = requirement.getResource();
			String bsn = getIdentityAttribute(r, "osgi.identity");
			if (bsn.equals(AGENT_LB_LEVEL4_BSN)) {
				if (requirement.getNamespace().equals("osgi.wiring.package")) {
					String filter = requirement.getDirectives().get("filter");
					if (filter != null && filter.indexOf("osgi.wiring.package=org.apache.logging") != -1) {
						return false;
					}
				}
			}
		}
		
		String ed = requirement.getDirectives().get(Namespace.REQUIREMENT_EFFECTIVE_DIRECTIVE);
		return ed == null || Namespace.EFFECTIVE_RESOLVE.equals(ed);
	}

	@Override
	public synchronized Map<Resource, Wiring> getWirings() { 
		if (! _localResources) return _localWirings;

		if(_wirings == null) {
			Map<Resource, Wiring> ws = new HashMap<Resource, Wiring>();
			Bundle[] bs = _bc.getBundles();
			for(Bundle b : bs) {
				BundleRevision br = b.adapt(BundleRevision.class);
				ws.put(br, br.getWiring());
			}
			_wirings = Collections.unmodifiableMap(ws);
		}
		return _wirings;
	}
	
	// Use all wirings from our local felix framework.
	private Map<Resource, Wiring> getLocalFrameworkWirings() {
		Map<Resource, Wiring> ws = new HashMap<Resource, Wiring>();
		for (Bundle b : _bc.getBundles()) {
			if (b.getBundleId() == 0 || b.getSymbolicName().equals("com.nokia.as.osgi.jre18")) {
				try {
					BundleRevision br = b.adapt(BundleRevision.class);
					ws.put(br, br.getWiring());
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		}
		return ws;
	}

	private <T> T getIdentityAttribute(Resource r, String attribute) {
		return (T) r.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).get(0).getAttributes().get(attribute);
	}

}
