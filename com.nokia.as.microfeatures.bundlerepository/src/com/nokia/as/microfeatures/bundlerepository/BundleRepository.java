// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.microfeatures.bundlerepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

/**
 * A Repository manager that contains bundles. A repository can manage multiple OBRs
 * which are resolved in the order in which they are declared.
 * 
 * (code adapter from knopflerfish repository API)
 */
public interface BundleRepository {
	
	/**
	 * Service property indicating the list of obr urls used for resolution.
	 * The property value is an url list (comma separated)
	 */
	public final static String OBR_URLS = "obr.urls";
	
	/**
	 * Service property indicating the local obr URL
	 */
	public final static String OBR_LOCAL = "obr.local";
	
	/**
	 * Service property indicating the sorted list of available obr url releases. The list is sorted in 
	 * ascending order, meaning the last entry is the latest release.
	 */
	public final static String OBR_RELEASES = "obr.releases";

	/**
	 * Service property indicating the obr urls that have been optionally set using the -Dobr system property.
	 * Null if the -D option is not set.
	 */
	public final static String OBR_CONFIGURED = "obr.configured";

	/**
	 * Return value from the install operation.
	 */
	public class InstallationResult {
		/**
		 * A set of resources that provide a resolution for the requested
		 * operation.
		 */
		public final Set<Resource> resources = new HashSet<Resource>();

		/**
		 * A list of messages suitable for user feedback.
		 */
		public final List<String> userFeedback = new ArrayList<String>();
	}
		
	/**
	 * Reload local OBR.
	 */
	void reloadLocalObr() throws Exception;
	
	/**
	 * Sets OBR urls
	 */
	void setObrUrls(List<String> obrUrls) throws Exception;

	/**
	 * Find providers for a requirement.
	 * 
	 * @see org.osgi.service.resolve.ResolverContext.findProviders
	 * @param requirement
	 *            {@link Requirement} to find providers for.
	 * @return
	 */
	List<Capability> findProviders(List<Requirement> requirements);
	
	/**
	 * Find providers for a requirement.
	 * 
	 * @see org.osgi.service.resolve.ResolverContext.findProviders
	 * @param requirement
	 *            {@link Requirement} to find providers for.
	 * @return
	 */
	default List<Capability> findProviders(Requirement ... requirements) {
		return findProviders(Arrays.asList(requirements));
	}

	/**
	 * Find a set of resources given the current state of the framework and
	 * using the currently enabled Repositories that will allow the given list
	 * of resources to resolve.
	 * 
	 * @param resources
	 *            List of {@link Resource} to find a resolution for.
	 * @param localResources
	 * 			  If false, ignore already installed bundles during the resolution. 
	 * @return A Set of {@link Resource} that will allow the given list of
	 *         resources to resolve.
	 * @throws Exception
	 *             If we failed to find a resolution.
	 */
	Set<Resource> findResolution(List<Resource> resources, boolean localResources) throws Exception;

	/**
	 * Find a set of resources given the current state of the framework and
	 * using the currently enabled Repositories that will allow the given list
	 * of resources to resolve.
	 * 
	 * @param resources
	 *            List of {@link Resource} to find a resolution for.
	 * @param localResources
	 * 			  If false, ignore already installed bundles during the resolution. 
	 * @param balcklistedResources the resources which must not be part of the resulting resolution
	 * @return A Set of {@link Resource} that will allow the given list of
	 *         resources to resolve.
	 * @throws Exception
	 *             If we failed to find a resolution.
	 */
	Set<Resource> findResolution(List<Resource> resources, boolean localResources, Set<Resource> blacklistedResources) throws Exception;

	/**
	 * Install, and optionally start, a list of {@link Resource}, and optionally
	 * try to find, install and start any {@link Resource} needed for them to
	 * resolve.
	 * 
	 * @param resources
	 *            List of {@link Resource} to install and optionally start
	 *            and/or find a resolution for.
	 * @param resolve
	 *            If true try to find a resolution for the given resources.
	 * @param start
	 *            If true start all resources and any dependencies found during
	 *            resolution.
	 * @return An InstallationResult containing a set of resources installed and
	 *         a list of messages suitable for user feedback.
	 * @throws Exception
	 *             If we failed to find a resolution or if install or start
	 *             operations failed.
	 */
	InstallationResult install(List<Resource> resources, boolean resolve, boolean start) throws Exception;
	
	/**
	 * Return a new RequirementBuilder which provides a convenient way to create a requirement.
	 */
	RequirementBuilder newRequirementBuilder(String namespace);
	
	/**
	 * Return a new RequirementBuilder which provides a convenient way to create a requirement.
	 */
	RequirementBuilder newRequirementBuilder(String namespace, String filter);

}
