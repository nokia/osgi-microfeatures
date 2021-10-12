// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.features.admin;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.VersionRange;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.http.HttpService;

import com.nokia.as.microfeatures.bundlerepository.BundleRepository;
import com.nokia.as.microfeatures.bundlerepository.RequirementBuilder;
import com.nokia.as.microfeatures.features.Feature;
import com.nokia.as.microfeatures.features.Feature.Type;
import com.nokia.as.microfeatures.features.FeatureRepository;

@Component
public class FeaturesServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;		
	private static final String CMD_PATH = "/cmd";	
	private static final String GUI_PATH = "/features";	
	private static final String OSGI_WIRING_PACKAGE = "osgi.wiring.package";	
	private static final MultipartConfigElement MULTI_PART_CONFIG = new MultipartConfigElement(System.getProperty("java.io.tmpdir"));
	
	@ServiceDependency
	HttpService _httpService;

	@ServiceDependency
	FeatureRepository _featuresRepo;
	
	@ServiceDependency
	BundleRepository _repos;

	@Inject
	BundleContext _bc;

	final static Logger _log = Logger.getLogger(FeaturesServlet.class);

	private String _defaultObrUrls;
	private String _localObr;
	
	@ServiceDependency
	void bind(BundleRepository repo, Map<String, Object> properties) {
		_repos = repo;
		/*
		 * When the user runs this tool by specifying obrs, these obrs will be taken.
		 * Else, a list of officials released obr will be used.
		 */
		_localObr = (String) properties.get(BundleRepository.OBR_LOCAL);
		String userObrs = (String) properties.get(BundleRepository.OBR_CONFIGURED);
		if( userObrs != null) {
			_defaultObrUrls = userObrs;
		} else {
			_defaultObrUrls = (String) properties.get(BundleRepository.OBR_RELEASES);
		}
	}
	
	@Start
	void start() {
		try {
			_log.info("Registering Features Servlet ...");
			HttpContextImpl hci = new HttpContextImpl(_bc.getBundle());
			_httpService.registerServlet(CMD_PATH, this, null, hci);
			AtomicReference<String> guiPath = new AtomicReference<String>(_bc.getProperty("gui.url.path"));
			if(guiPath.get() == null || "".equals(guiPath.get().trim())){
				guiPath.set(GUI_PATH);
			}
			if(!guiPath.get().startsWith("/")){
				guiPath.set("/"+guiPath.get());
			}
			if(_log.isDebugEnabled()){
				_log.debug("Using gui path: "+guiPath.get());
			}
			_httpService.registerResources(guiPath.get().trim(), "resources", hci);	
			System.out.println("Microfeature web admin ready (http://localhost:9090/features/index.html)");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {

			List<String> paths = getPathInfo(req);

			if (_log.isDebugEnabled()) {
				_log.debug("doGet: request paths=" + paths);
			}

			if (paths.size() == 3 && paths.get(0).equals("default") && paths.get(1).equals("obr")
					&& paths.get(2).equals("list")) {
				getDefaultObr(req, resp);
				return;
			} else if (paths.size() == 2 && paths.get(0).equals("list")) {
				getFeatures(req, resp, paths.get(1));
				return;
			} else if (paths.size() == 3 && paths.get(0).equals("obr") && 
					paths.get(2).equals("bundles")) {
				getBundles(req, resp, paths.get(1).trim());
				return;
			} else if (paths.size() == 4 && paths.get(0).equals("bundle") && 
					paths.get(3).equals("resolution")) {
				getBundleResolution(req, resp, paths.get(1).trim(),paths.get(2).trim());
				return;
			}  else if (paths.size() == 4 && paths.get(0).equals("bundle") && 
					paths.get(3).equals("dependent")) {
				getDependentBundles(req, resp, paths.get(1).trim(),paths.get(2).trim());
				return;
			} else {
				// bad request
				resp.setStatus(400);
			}

		} catch (Exception e) {
			e.printStackTrace();
			try {
				respondError(resp, e);
			} catch (Exception ej) {}
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		List<String> paths = getPathInfo(req);
		try {
			if (_log.isDebugEnabled()) {
				_log.debug("doPost: request paths=" + paths+", content-type="+req.getContentType());
			}
			if (paths.size() >= 1 && "runtime".equals(paths.get(0))) {
				createRuntime(req, resp);
			} else if (paths.size() >= 1 && "assembly".equals(paths.get(0))){
				createAssembly(req, resp);
			} else if (paths.size() >= 1 && "snapshot".equals(paths.get(0))){	
				createSnapShot(req, resp);	
			} else if (paths.size() == 2 && "repos".equals(paths.get(0)) && "reload".equals(paths.get(1))){	
				reloadRepos(req, resp);	
			} else {
				// bad request
				resp.setStatus(400);
			}
		} catch (Exception e) {
			e.printStackTrace();
			try {
				respondError(resp, e);
			} catch (Exception ej) {}
		}
	}
	
	
	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		List<String> paths = getPathInfo(req);
		try {
			if (_log.isDebugEnabled()) {
				_log.debug("doDelete: request paths=" + paths+", content-type="+req.getContentType());
			}
			if (paths.size() >= 1 && "assembly".equals(paths.get(0))){
				deleteFeature(req, resp, Feature.Type.ASMB);
			} else if (paths.size() >= 1 && "snapshot".equals(paths.get(0))){
				deleteFeature(req, resp, Feature.Type.SNAPSHOT);	
			} else {
				// bad request
				resp.setStatus(400);
			}
		} catch (Exception e) {
			e.printStackTrace();
			try {
				respondError(resp, e);
			} catch (Exception ej) {}
		}
	}

	private void getDefaultObr(HttpServletRequest req, HttpServletResponse resp) throws Exception {
		JSONObject result = new JSONObject();
		JSONArray obrs = new JSONArray();
		for (String obr : _defaultObrUrls.split(",")) {
			obr = obr.trim();
			if (!"".equals(obr)) {
				obrs.put(obr);
			}
		}
		result.put("obrs", obrs);
		result.put("localobr", _localObr);
		resp.setContentType("application/json;charset=UTF-8");
		resp.setHeader("Cache-Control", "no-cache");
		result.write(resp.getWriter());

	}
	
	private void getBundles(HttpServletRequest req, HttpServletResponse resp, String obrUrlString) throws Exception {
		
		String obrUrlDecoded = URLDecoder.decode(obrUrlString,"UTF-8");
		if (_log.isDebugEnabled()) {
			_log.debug("getBundles: "+obrUrlDecoded);
		}
		URL obrUrl = new URL(obrUrlDecoded);
        URLConnection connection = obrUrl.openConnection();
        try(BufferedReader in = new BufferedReader(
        		new InputStreamReader(connection.getInputStream()))){
        	StringBuilder sb = new StringBuilder();
	        String inputLine;
	        while ((inputLine = in.readLine()) != null){
	        	sb.append(inputLine);
	        }
	        resp.setStatus(200);
	        resp.setContentType(connection.getContentType());
	        resp.getWriter().write(sb.toString());
        }
        if (_log.isDebugEnabled()) {
			_log.debug("getBundles: done");
		}
	}
	
	
	private void getDependentBundles(HttpServletRequest req, HttpServletResponse resp, 
			String bsn, String version) throws Exception {
		
		BundleRepository repo = getBundleRepository(req);
		boolean all = req.getParameter("all") != null;
		if (_log.isDebugEnabled()) {
			_log.debug("getDependentBundles: bsn="+bsn+", version="+version+", all="+all);
		}
		
		// first get matching bundle		
		RequirementBuilder requirementBuilder = repo.newRequirementBuilder(IdentityNamespace.IDENTITY_NAMESPACE,bsn);
		if (version != null && version.length() > 0 && !"none".endsWith(version)) {
			requirementBuilder.addVersionRangeFilter(new VersionRange(version));
		}
		requirementBuilder.addBundleIdentityFilter();
		List<Capability> cs = repo.findProviders(requirementBuilder.build());
		JSONObject json = new JSONObject();
		JSONArray resources = new JSONArray();
		json.put("resources", resources);
		if (!cs.isEmpty()) {
			Resource resource = cs.get(0).getResource();
			// get all exported packages
			List<Package> packages = resource.getCapabilities(OSGI_WIRING_PACKAGE).stream()
					.map(rcs -> 					
						new Package((String)rcs.getAttributes().get(OSGI_WIRING_PACKAGE), 
								rcs.getAttributes().containsKey(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE) ? 
										rcs.getAttributes().get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE).toString() : null)
					).collect(Collectors.toList());
			
			if (_log.isDebugEnabled()) {
				_log.debug("getDependentBundles: bsn="+bsn+", version="+version+", found exported packages: "+packages);
			}
			Set<Resource> dependingResources = new HashSet<Resource>();
			if(!packages.isEmpty()){
				//Retrieve all resources				
				requirementBuilder = repo.newRequirementBuilder(IdentityNamespace.IDENTITY_NAMESPACE,"*");
				requirementBuilder.addBundleIdentityFilter();
				cs = repo.findProviders(requirementBuilder.build());
				if (!cs.isEmpty()) {					
					cs.forEach(c -> {						
						Resource res = c.getResource();
						if(res.getRequirements(OSGI_WIRING_PACKAGE).stream().anyMatch(rr ->{
							try {
								String filterString = rr.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
								org.osgi.framework.Filter filter = _bc.createFilter(filterString);
								if(	packages.stream().anyMatch(
										p -> {
											Dictionary<String, String> d = new Hashtable<String, String>();
											d.put(OSGI_WIRING_PACKAGE, p.getName());
											if (p.getVersion() != null) {
												d.put(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, p.getVersion());
											}
											if(filter.match(d)){
												return true;
											}
											return false;
										}
										)){
									return true;
								}
							} catch (InvalidSyntaxException e) {
								e.printStackTrace();
							}
							return false;
						})){
							dependingResources.add(res);
						}
					});
				}
			}
			fillBlueprintResources(resources, dependingResources, IdentityNamespace.IDENTITY_NAMESPACE, all);			
		}
		resp.setContentType("application/json;charset=UTF-8");
		resp.setHeader("Cache-Control", "no-cache");
		json.write(resp.getWriter());
	}
	
	private void getBundleResolution(HttpServletRequest req, HttpServletResponse resp, 
			String bsn, String version) throws Exception {
		
		BundleRepository repo = getBundleRepository(req);
		boolean all = req.getParameter("all") != null;
		if (_log.isDebugEnabled()) {
			_log.debug("getBundleResolution: bsn="+bsn+", version="+version+", all="+all);
		}
		
		RequirementBuilder requirementBuilder = repo.newRequirementBuilder(IdentityNamespace.IDENTITY_NAMESPACE,bsn);
		if (version != null && version.length() > 0 && !"none".endsWith(version)) {
			requirementBuilder.addVersionRangeFilter(new VersionRange(version));
		}
		requirementBuilder.addBundleIdentityFilter();
		List<Capability> cs = repo.findProviders(requirementBuilder.build());
		JSONObject json = new JSONObject();
		JSONArray resources = new JSONArray();
		json.put("resources", resources);
		if (!cs.isEmpty()) {
			Resource resource = cs.get(0).getResource();
			Set<Resource> resolvedResources = repo.findResolution(Collections.singletonList(resource),false);
			fillBlueprintResources(resources, resolvedResources, IdentityNamespace.IDENTITY_NAMESPACE, all);
		}
		
		resp.setContentType("application/json;charset=UTF-8");
		resp.setHeader("Cache-Control", "no-cache");
		json.write(resp.getWriter());
	}
	
	private void fillBlueprintResources(JSONArray json, Collection<Resource> resources, 
			String namespace, boolean verbose) throws Exception{
		
		for (Resource r : resources) {
			JSONObject resource = new JSONObject();
			json.put(resource);						
			final List<Capability> capabilities = r.getCapabilities(namespace);
			Map<String, Object> nsAttrs = capabilities.iterator().next().getAttributes();
			resource.put("bsn", nsAttrs.get(namespace));
			resource.put("version", nsAttrs.get("version"));
			
			if (verbose) {
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
				
				JSONArray jscaps = new JSONArray();
				resource.put("caps",jscaps);
				for (Capability rc : caps) {
					JSONObject jscap = new JSONObject();
					jscaps.put(jscap);
					jscap.put("ns", rc.getNamespace());
					JSONObject attributes =  new JSONObject();
					JSONObject directives =  new JSONObject();
					jscap.put("attributes", attributes);
					jscap.put("directives", directives);
					fillJsonObject(attributes, rc.getAttributes());
					fillJsonObject(directives, rc.getDirectives());
				}
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
				JSONArray jsreqs = new JSONArray();
				resource.put("reqs",jsreqs);
				for (Requirement rr : r.getRequirements(null)) {
					JSONObject jsreq = new JSONObject();
					jsreqs.put(jsreq);
					jsreq.put("ns", rr.getNamespace());
					JSONObject attributes =  new JSONObject();
					JSONObject directives =  new JSONObject();
					jsreq.put("attributes", attributes);
					jsreq.put("directives", directives);
					fillJsonObject(attributes, rr.getAttributes());
					fillJsonObject(directives, rr.getDirectives());
				}
			}
		}		
	}
	
	private void fillJsonObject(JSONObject json, Map<String, ?> m) {
		for (Entry<String, ?> e : m.entrySet()) {
			try {
				json.put(e.getKey(),e.getValue());
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
		}
	}

	private void getFeatures(HttpServletRequest req, HttpServletResponse resp, String typeParam) throws Exception {

		JSONObject result = new JSONObject();
		JSONArray features = new JSONArray();
		FeatureRepository fr = getFeatureRepository(req);
		
		Feature.Type type = Feature.Type.FEATURE;
		if(typeParam != null && !"".equals(typeParam.trim())){
			type = Feature.Type.valueOf(typeParam.trim().toUpperCase());
		}
		
		if (_log.isDebugEnabled()) {
			_log.debug("getFeatures: type="+type);
		}
		
		for (Feature f : fr.findFeatures(type)) {
			
			JSONObject jf = new JSONObject();
			jf.put("name", f.getName());
			jf.put("bsn", f.getSymbolicName());
			jf.put("version", f.getVersion());
			jf.put("url", f.getDoc() != null ? f.getDoc().toString():"");
			jf.put("desc", f.getDesc() != null ? f.getDesc().toString():"");
			
			if(type == Type.ASMB){
				JSONArray asmbFeatures = new JSONArray();
				// get related features
				RequirementBuilder requirementBuilder = _repos.newRequirementBuilder(IdentityNamespace.IDENTITY_NAMESPACE);
				requirementBuilder.addDirective("filter", "(" + IdentityNamespace.IDENTITY_NAMESPACE + "=" + f.getSymbolicName() + ")");
				requirementBuilder.addVersionRangeFilter(new VersionRange("["+f.getVersion()+","+f.getVersion()+"]"));
				_repos.findProviders(requirementBuilder.build()).stream()
							.map(c -> c.getResource())
							.map(rs -> rs.getRequirements(Feature.NAMESPACE))
							.forEach(reqlist -> reqlist
									.forEach(
											requ -> { 
												Optional.ofNullable((String)requ.getDirectives().get("filter"))
												.ifPresent(fs -> asmbFeatures.put(fs));													
											}
									)			
							);	
				jf.put("asmbfeatures", asmbFeatures);
			} else if (type == Type.SNAPSHOT){
				// get assembly reference
				f.getAttributes(Feature.ASMB_BSN).ifPresent(bsn -> {
					try {
						jf.put("asmbbsn", bsn.toString());
					} catch (JSONException e) {}
				} );
				
				f.getAttributes(Feature.ASMB_VERSION).ifPresent(bsn -> {
					try {
						jf.put("asmbversion", bsn.toString());
					} catch (JSONException e) {}
				} );
			}			
			
			JSONArray categories = new JSONArray();
			f.getCategories().forEach(cat -> categories.put(cat));
			features.put(jf);
			jf.put("categories", categories);
		}

		result.put("features", features);
		resp.setContentType("application/json;charset=UTF-8");
		resp.setHeader("Cache-Control", "no-cache");
		result.write(resp.getWriter());
	}
	
	
	private void createAssembly(HttpServletRequest req, HttpServletResponse resp) throws Exception {
		try {
			JSONObject json = getJSONContent(req);
			
			if (_log.isDebugEnabled()) {
				_log.debug("createAssembly: got json object: "+json);
			}
			
			List<FeatureQuery> featuresQuery = getFeatures(json,"features");
			String name = getJsonStringValue(json,"name");
			String desc = getJsonStringValue(json,"desc");
			String version = getJsonStringValue(json,"version");
			String doc = getJsonStringValue(json,"doc");
			
			if (_log.isDebugEnabled()) {
				_log.debug("createAssembly: features=" + featuresQuery +", name="
						+ name + ", version=" + version + ", desc=" + desc+", doc="+doc);
			}
			
			if (featuresQuery == null || featuresQuery.isEmpty()) {
				_log.warn("createAssembly: invalid request, no feature set");
				// bad request
				resp.setStatus(400);
				return;
			}		
			
			FeatureRepository fr = getFeatureRepository(req);
			
			List<Feature> featureList = new ArrayList<>();
			Set<Feature> availableFeatures = fr.findFeatures(Type.FEATURE);
			availableFeatures.forEach(feature -> {
				featuresQuery.forEach(
						fquery -> {
							if(fquery.getName().equals(feature.getName())
									&& (fquery.getVersion().equals(feature.getVersion()))
									){
								featureList.add(feature);
							}
						}
				);
			});
			
			if (featuresQuery.size() != featureList.size()) {
				_log.warn("createAssembly: invalid request, could not retrieve requested features");
				// bad request
				resp.setStatus(400);
				return;
			}	
			
			
			if (_log.isDebugEnabled()) {
				_log.debug("createAssembly: will assemble features: "+featureList);
			}

			if (name == null) {
				name = featureList.get(0).getName().toLowerCase();
			}
			if (version == null) {
				version = "1.0.0";
			}
			if (desc == null) {
				desc = name;
			}
			
			fr.createAssembly(desc, name, version, doc, featureList);
			resp.setStatus(200);
			
		} catch (Exception e) {
			e.printStackTrace();
			try {
				respondError(resp, e);
			} catch (Exception ej) {
			}
		}
	}
		
	private void reloadRepos(HttpServletRequest req, HttpServletResponse resp)
			throws Exception {
		if (_log.isDebugEnabled()) {
			_log.debug("reloadRepos");
		}
		//reload repos
		_repos.reloadLocalObr();
		
		// Retrieves from URL parameters the current obrs		
		List<String> obrs = getObrUrls(req);
		if( obrs != null ) {
			_repos.setObrUrls(obrs);

			if (_log.isDebugEnabled()) {
				_log.debug("reloadRepos: done");
			}
			resp.setStatus(200);
		} else {
			if (_log.isDebugEnabled()) {
				_log.debug("reloadRepos: internal error, no obr url in request parameters");
			}
			resp.setStatus(400);
		}
	}	
	
	private JSONObject getJsonFromBody(HttpServletRequest req) throws Exception {
		BufferedInputStream in = new BufferedInputStream(req.getInputStream());
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int b;
		while ((b = in.read()) != -1) {
			out.write(b);
		}
		return new JSONObject(new String(out.toByteArray()));
	}
	
	private List<String> getObrUrls(final HttpServletRequest req) {
		String obrs = req.getParameter("obrs");
		if(obrs == null){
			try{
				JSONObject json = getJsonFromBody(req);
				obrs = json.getString("obrs");
			} catch(Exception e){
				e.printStackTrace();
			}
		}
		if (_log.isDebugEnabled()) {
			_log.debug("getObrUrls: obrs=" + obrs);
		}
		if (obrs == null || "".equals(obrs.trim())) {
			try {
				JSONObject json = getJSONContent(req);				
				if (_log.isDebugEnabled()) {
					_log.debug("getObrUrls: got json object: "+json);
				}
				obrs = getJsonStringValue(json,"obrs");

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if( obrs != null) {
			List<String> urls = new ArrayList<>();
			for (String obr : obrs.split(",")) {
				urls.add(obr);
			}
			return urls;
		}
		return null;
	}
	
	private void deleteFeature(HttpServletRequest req, HttpServletResponse resp, Feature.Type type)
			throws Exception {
		try {
			JSONObject json = getJSONContent(req);
			
			if (_log.isDebugEnabled()) {
				_log.debug("deleteFeature: got json object: "+json+", type="+type);
			}
			
			String bsn = getJsonStringValue(json,"bsn");
			String version = getJsonStringValue(json,"version");
			
			if (_log.isDebugEnabled()) {
				_log.debug("deleteFeature: bsn="+bsn+", version=" + version);
			}
			
			if (bsn == null || version == null) {
				_log.warn("deleteFeature: invalid request, no feature set");
				// bad request
				resp.setStatus(400);
				return;
			}		
			
			FeatureRepository fr = getFeatureRepository(req);
			fr.delete(type.name(), bsn, version);
			resp.setStatus(200);
			
		} catch (Exception e) {
			e.printStackTrace();
			try {
				respondError(resp, e);
			} catch (Exception ej) {
			}
		}
	}
	
	private void createSnapShot(HttpServletRequest req, HttpServletResponse resp)
			throws Exception {
		try {
			JSONObject json = getJSONContent(req);
			
			if (_log.isDebugEnabled()) {
				_log.debug("createSnapShot: got json object: "+json);
			}
			
			String bsn = getJsonStringValue(json,"bsn");
			String version = getJsonStringValue(json,"version");
			String doc = getJsonStringValue(json,"doc");
			
			if (_log.isDebugEnabled()) {
				_log.debug("createSnapShot: bsn="+bsn+", version=" + version +", doc="+doc);
			}
			
			if (bsn == null || version == null) {
				_log.warn("createSnapShot: invalid request, no feature set");
				// bad request
				resp.setStatus(400);
				return;
			}		
			
			FeatureRepository fr = getFeatureRepository(req);
			fr.createSnapshot(bsn, version, doc);	
			resp.setStatus(200);
			
		} catch (Exception e) {
			e.printStackTrace();
			try {
				respondError(resp, e);
			} catch (Exception ej) {
			}
		}
	}	
	
	private void createRuntime(HttpServletRequest req, HttpServletResponse resp)
			throws Exception {
		File bpDeployOutput = null;
		try {
			JSONObject json = getJSONContent(req);
			
			if (_log.isDebugEnabled()) {
				_log.debug("createRuntime: got json object: "+json);
			}
			
			String bsn = getJsonStringValue(json,"bsn");
			String version = getJsonStringValue(json,"version");
			
			String p = getJsonStringValue(json,"p"), g = null, c = null, i = null;
			if(p != null){
				g = getJsonStringValue(json,"g");
				c = getJsonStringValue(json,"c");
				i = getJsonStringValue(json,"i");
				
				if(c == null || g == null || i == null){
					_log.warn("createRuntime: invalid request, p not null but no g/c/i");
					// bad request
					resp.setStatus(400);
					return;
				}
				if (_log.isDebugEnabled()) {
					_log.debug("createRuntime: bsn="+bsn+", version=" + version+", p="+p+", g="+g+", c="+c+", i="+i);
				}
			} else if (_log.isDebugEnabled()) {
				_log.debug("createRuntime: bsn="+bsn+", version=" + version);
			}
			
			if (bsn == null || version == null) {
				_log.warn("createRuntime: invalid request, no feature set");
				// bad request
				resp.setStatus(400);
				return;
			}		
						
			FeatureRepository fr = getFeatureRepository(req);
			if(p == null){
				fr.createRuntime(bsn, version);
			} else {
				if (_log.isDebugEnabled()) {
					_log.debug("createRuntime: using legacy layer");
				}
				fr.createRuntime(bsn, version, p, g, c, i);
			}
			
			if (_log.isDebugEnabled()) {
				_log.debug("createRuntime: runtime created, zipping directory");
			}
			// strip last .snapshot part in bsn in order to generate the zip without bsn.snapshot-x.y.z.zip ...
			int snapshotIndex = bsn.lastIndexOf(".snapshot");
			if (snapshotIndex != -1) {
				bsn = bsn.substring(0, snapshotIndex);
			}
			String bpDirName = bsn +"-"+ version;
			// zip local instances directory	
			String zipName =  bpDirName + ".zip";
			bpDeployOutput = new File(System.getProperty("java.io.tmpdir"), zipName);
			
			respondFile(resp, bpDeployOutput.getName(), bpDeployOutput, "application/zip", req.getParameter("cid"));			
		} catch (Exception e) {
			e.printStackTrace();
			try {
				respondError(resp, e);
			} catch (Exception ej) {
			}
		} finally {
			if (bpDeployOutput != null && bpDeployOutput.exists()) {
				bpDeployOutput.delete();
			}
		}		
	}

	private static void respondFile(HttpServletResponse resp, String name, File file, String contentType, String cookieId)
			throws Exception {

		resp.setStatus(200);
		resp.setContentType(contentType);
		resp.setContentLength((int) file.length()); 		
		resp.setHeader("Content-disposition", "attachment; filename=" + name);
		
		if (cookieId != null) {
			Cookie cookie = new Cookie(cookieId, "ok");
			cookie.setPath("/");
			cookie.setMaxAge(600);
			resp.addCookie(cookie);
		}
		
		try(FileInputStream in = new FileInputStream(file); OutputStream out = new BufferedOutputStream(resp.getOutputStream())){
			byte[] buffer = new byte[4096];
			int length;
			while ((length = in.read(buffer)) > 0) {
				out.write(buffer, 0, length);
			}
			out.close();
		}
	}

	private void respondError(HttpServletResponse resp, Exception e) throws Exception {
		respondError(resp,e.toString());
	}
	
	private void respondError(HttpServletResponse resp, String message) throws Exception {
		resp.setStatus(500);
		JSONObject result = new JSONObject();
		result.put("error", message);
		resp.setContentType("application/json;charset=UTF-8");
		result.write(resp.getWriter());
	}

	private static void removeDirectory(File dir) {
		if (dir.isDirectory()) {
			File[] files = dir.listFiles();
			if (files != null && files.length > 0) {
				for (File aFile : files) {
					removeDirectory(aFile);
				}
			}
			dir.delete();
		} else {
			dir.delete();
		}
	}

	private FeatureRepository getFeatureRepository(final HttpServletRequest req) throws Exception {
		FeatureRepository fr = _featuresRepo;
		setObrUrls(req,fr);
		return fr;
	}
	
	private BundleRepository getBundleRepository(final HttpServletRequest req) throws Exception {
		BundleRepository repo = _repos;
		setObrUrls(req,repo);
		return repo;
	}
	

	private void setObrUrls(final HttpServletRequest req, final FeatureRepository fr) throws Exception {
		//fr.setObrs(getObrUrls(req));
		// FIXME not supported anymore: obs are now only configured from bnd or cnf files
	}
	
	private void setObrUrls(final HttpServletRequest req, final BundleRepository repo) throws Exception {
		// repo.setObrs(getObrUrls(req));
		// FIXME not supported anymore: obs are now only configured from bnd or cnf files
	}
	
	private List<String> getPathInfo(HttpServletRequest request) throws UnsupportedEncodingException {
		List<String> paths = new ArrayList<String>();
		String requestUri = request.getRequestURI().substring(request.getServletPath().length());
		while (requestUri.startsWith("/")) {
			requestUri = requestUri.substring(1);
		}
		int queryIndex = requestUri.indexOf("?");
		if (queryIndex == -1) {
			queryIndex = requestUri.indexOf("&");
		}
		if (queryIndex > -1) {
			requestUri = requestUri.substring(0, queryIndex);
		}
		for (String path : requestUri.split("/")) {
			paths.add(URLDecoder.decode(path.trim(),"UTF-8"));
		}
		return paths;
	}

	private JSONObject getJSONContent(final HttpServletRequest req) throws Exception{
		boolean isJson = req.getContentType() != null && req.getContentType().trim().startsWith("application/json");
		JSONObject json = null;		
		if(isJson){
			StringBuilder jsonContent = new StringBuilder();
			String line = null;
			BufferedReader reader = req.getReader();
			while ((line = reader.readLine()) != null){
					jsonContent.append(line);
			}
			json = new JSONObject(jsonContent.toString());
		} else {
			String jsonParam = req.getParameter("json");
			if(jsonParam != null){
				json = new JSONObject(jsonParam);
			} else if (req.getContentType() != null && req.getContentType().startsWith("multipart/form-data")) {				
				req.setAttribute("org.eclipse.jetty.multipartConfig", MULTI_PART_CONFIG);
				Part filePart = req.getPart("json");
				if(filePart != null){
					try(Scanner scan = new Scanner(filePart.getInputStream())){
						java.util.Scanner s = scan.useDelimiter("\\A");
						jsonParam = s.hasNext() ? s.next() : "";
						json = new JSONObject(jsonParam);
					}
				}				
			}
		}
		return json;
	}
	
	
	private void copy(File sourceLocation, File targetLocation) throws IOException {
	    if (sourceLocation.isDirectory()) {
	        copyDirectory(sourceLocation, targetLocation);
	    } else {
	        copyFile(sourceLocation, targetLocation);
	    }
	}

	private void copyDirectory(File source, File target) throws IOException {
	    if (!target.exists()) {
	        target.mkdir();
	    }

	    for (String f : source.list()) {
	        copy(new File(source, f), new File(target, f));
	    }
	}

	private void copyFile(File source, File target) throws IOException {        
	    try (
	            InputStream in = new FileInputStream(source);
	            OutputStream out = new FileOutputStream(target)
	    ) {
	        byte[] buf = new byte[1024];
	        int length;
	        while ((length = in.read(buf)) > 0) {
	            out.write(buf, 0, length);
	        }
	    }
	}
		
	private String getJsonStringValue(final JSONObject json, final String key){
		if(json != null && json.has(key)){
			try{
				return json.getString(key);
			}
			catch(Exception e){}
		}
		return null;
	}
	
	private List<FeatureQuery> getFeatures(final JSONObject json, final String key){
		if(json != null && json.has(key)){
			try{
				List<FeatureQuery> list = new ArrayList<FeatureQuery>();
				JSONArray array = json.getJSONArray(key);
				for(int i=0;i<array.length();i++){
					JSONObject f = array.getJSONObject(i);
					list.add(
							new FeatureQuery(f.getString("name"), f.getString("version"))
					);
				}
				return list;
			}
			catch(Exception e){}
		}
		return null;
	}
	
	private String getStringFromInputStream(final InputStream is) {
		if(is == null){
			return null;
		}
		BufferedReader br = null;
		StringBuilder sb = new StringBuilder();

		String line;
		try{
			br = new BufferedReader(new InputStreamReader(is));
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return sb.toString();
	}
	
	private class Package {
		private String _name;
		private String _version;
		
		private Package(String name, String version){
			_name = name;
			_version = version;
		}
		
		private String getName() {
			return _name;
		}
		
		private String getVersion() {
			return _version;
		}
		
		public String toString(){
			return new StringBuilder("[name=").append(getName())
					.append(", version=").append(getVersion()).append("]").toString();
		}
	}
	
	private static class FeatureQuery{
		private String _name, _version;
		
		private FeatureQuery(String name, String version){
			_name = name;
			_version = version;
		}

		private String getName() {
			return _name;
		}

		private String getVersion() {
			return _version;
		}
		
		public String toString(){
			return new StringBuilder("name=").append(getName())
					.append(", version=").append(getVersion()).toString();
		}
	}
}
