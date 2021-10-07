package com.nokia.as.microfeatures.features;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.resource.Resource;

import com.nokia.as.microfeatures.features.Feature.Type;

/**
 * A Repository that contains Features.
 * Allows to find, resolve features, generate application assembly or install a complete application.
 */
@ProviderType
public interface FeatureRepository {
	
	// find all features with type=FEATURE
	Set<Feature> findFeatures() throws Exception;
		
	// find all categories from all features
	Set<String> findCategories() throws Exception;
	
	// find all non internal features with a given type
	Set<Feature> findFeatures(Feature.Type type) throws Exception;
	
	// find all features with a given type.
	Set<Feature> findFeatures(Feature.Type type, boolean internal) throws Exception;
	
	// find a feature from its name/version
	Optional<Feature> findFeature(Type type, String name, String version);
	
	// create an assembly of features.
	void createAssembly(String name, String bsn, String version, String doc, List<Feature> features) throws Exception;	
				
	// Fires an osgi resolution on a given assembly of features, and save the result in a snapshot
	void createSnapshot(String bsn, String version, String doc) throws Exception;
	
	// Fires an osgi resolution on a given assembly of features, and save the result in a snapshot
	Set<Resource> resolveSnapshot(String bsn, String version, String doc) throws Exception;

	// Create a snapshot with a specified list of resources
	void createSnapshot(String bsn, String version, List<Resource> resources) throws Exception;		
	
	// Creates a runtime based on a given snapshot
	void createRuntime(String bsn, String version) throws Exception;
	
	// Creates a runtime based on a given snapshot, using old blueprint format (p/g/c/i)
	void createRuntime(String bsn, String version, String p, String g, String c, String i) throws Exception;
	
	// Deletes a feature from the local obr
	void delete(String type, String bsn, String version) throws Exception;

	// Append to a list of existing features some default features (like lib.jre features which depends on current jdk, etc ...)
	void checkDefaultFeatures(List<Feature> features);

	// Calculate black listed resources which are referenced by some features.
	Set<Resource> resolveBlacklistedResources(List<String> filters);

}
