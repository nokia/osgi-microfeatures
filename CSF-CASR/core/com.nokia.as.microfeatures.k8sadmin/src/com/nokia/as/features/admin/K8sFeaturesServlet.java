package com.nokia.as.features.admin;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import java.util.stream.Collectors;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.VersionRange;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import com.alcatel.as.service.log.LogService;
import com.alcatel.as.service.log.LogServiceFactory;
import com.nokia.as.features.admin.k8s.Deployer;
import com.nokia.as.features.admin.k8s.Function;
import com.nokia.as.features.admin.k8s.Route;
import com.nokia.as.microfeatures.bundlerepository.BundleRepository;
import com.nokia.as.microfeatures.bundlerepository.RequirementBuilder;
import com.nokia.as.microfeatures.features.Feature;
import com.nokia.as.microfeatures.features.Feature.Type;
import com.nokia.as.microfeatures.features.FeatureRepository;

@Component(provides = K8sFeaturesServlet.class)
@Path("/")
public class K8sFeaturesServlet {

	public static final String CURRENT_NAMESPACE = System.getProperty("k8s.namespace", "namespace"); 
	private static final String OSGI_WIRING_PACKAGE = "osgi.wiring.package";	
	private static final MultipartConfigElement MULTI_PART_CONFIG = new MultipartConfigElement(System.getProperty("java.io.tmpdir"));
	
	@ServiceDependency
	FeatureRepository featuresRepo;
	
	@ServiceDependency
	BundleRepository repos;
	
	@ServiceDependency
	Deployer deployer;
	
	@ServiceDependency
	LogServiceFactory logFactory;
	LogService logger;

	@Inject
	BundleContext bc;

	private String defaultObrUrls;
	private String localObr;
	
	@ServiceDependency
	void bind(BundleRepository repo, Map<String, Object> properties) {
		repos = repo;
		
		/* When the user runs this tool by specifying obrs, these obrs will be taken.
		 * Else, a list of officials released obr will be used */
		localObr = (String) properties.get(BundleRepository.OBR_LOCAL);
		String userObrs = (String) properties.get(BundleRepository.OBR_CONFIGURED);
		if( userObrs != null) {
			defaultObrUrls = userObrs;
		} else {
			defaultObrUrls = (String) properties.get(BundleRepository.OBR_RELEASES);
		}
	}
	
	@Start
	void start() throws Exception {
		logger = logFactory.getLogger(K8sFeaturesServlet.class);
		logger.info("Microfeatures WebAdmin ready (/features/index.html)");
	}

	@GET
	@Path("features/{path : .*}")
	public InputStream resources(@PathParam("path") String path) {
		return K8sFeaturesServlet.class.getResourceAsStream("/resources/" + path);
	}
	
	@GET
	@Path("cmd/sessioninfo")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSessionInfo(@Context HttpServletRequest request) {
		logger.debug("K8sFeaturesServlet::getSessionInfo");
		KeycloakSecurityContext securityContext = (KeycloakSecurityContext) request.getAttribute(KeycloakSecurityContext.class.getName());
		try {
			JSONObject result = new JSONObject();
			if (securityContext != null) {
				AccessToken token = securityContext.getToken();
				result.put("preferredUsername", token.getPreferredUsername());
				result.put("name", token.getName());
				AccessToken.Access access =  token.getRealmAccess();
				if (access != null) {
					result.put("roles", access.getRoles());
				}
			} else if (Boolean.getBoolean("standalone")) {
				JSONArray roles = new JSONArray();
				roles.put("admin");
				result.put("preferredUsername", "user1");
				result.put("name", "User1");
				result.put("roles", roles);
			}
			
			return Response.ok(result.toString())
					   .header("Cache-Control", "no-cache")
					   .build();
		} catch(JSONException e) {
			logger.warn("K8sFeaturesServlet::getSessionInfo", e);
			return Response.serverError().build();
		}
	}
	
	@GET
	@Path("/cmd/disconnect")
	public Response disconnectSession(@Context HttpServletRequest request) {
		logger.debug("K8sFeaturesServlet::disconnectSession");
		try {
			request.logout();
			return Response.ok()
					   .header("Cache-Control", "no-cache")
					   .build();
		} catch (ServletException e) {
			logger.warn("K8sFeaturesServlet::disconnectSession", e);
			return Response.serverError().build();
		}
	}
	
	@GET
	@Path("/cmd/deployeds")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getDeployedRuntimes() {
		logger.debug("K8sFeaturesServlet::getDeployedRuntimes");
		try {
			JSONObject result = new JSONObject();
			JSONArray runtimes = new JSONArray();
			
			for(com.nokia.as.features.admin.k8s.Runtime r: deployer.deployedRuntimes()) {
				JSONObject jr = r.rawJSON;
				jr.put("status", r.status().statusStr());
				jr.put("podsUrls", r.getPodsUrls());
				//jr.put("pods", r.pods())
				runtimes.put(jr);
			}
	
			result.put("runtimes", runtimes);
			return Response.ok(result.toString())
					   .header("Cache-Control", "no-cache")
					   .build();
		} catch(JSONException e) {
			logger.warn("K8sFeaturesServlet::getDeployedRuntimes", e);
			return Response.serverError().build();
		}
	}
	
	@GET
	@Path("/cmd/routes")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getCreatedRoutes() {
		logger.debug("K8sFeaturesServlet::getCreatedRoutes");
		try {
			JSONObject result = new JSONObject();
			JSONArray routes = new JSONArray();

			deployer.deployedRoutes().stream()
					.filter(f -> f != null)
					.map(f -> f.rawJSON)
					.forEach(routes::put);

			result.put("routes", routes);
			return Response.ok(result.toString())
					   .header("Cache-Control", "no-cache")
					   .build();
		} catch(JSONException e) {
			logger.warn("K8sFeaturesServlet::getCreatedRoutes", e);
			return Response.serverError().build();
		}
	}
	
	@GET
	@Path("/cmd/functions")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getCreatedFunctions() {
		logger.debug("K8sFeaturesServlet::getCreatedFunctions");
		try {
			JSONObject result = new JSONObject();
			JSONArray functions = new JSONArray();
			
			deployer.deployedFunctions().stream()
					.filter(f -> f != null)
					.map(f -> f.rawJSON)
					.forEach(functions::put);
			
			result.put("functions", functions);
			return Response.ok(result.toString())
					   .header("Cache-Control", "no-cache")
					   .build();
		} catch(JSONException e) {
			logger.warn("K8sFeaturesServlet::getCreatedFunctions", e);
			return Response.serverError().build();
		}
	}
	
	@GET
	@Path("cmd/list/{param : .*}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getFeatures(@PathParam("param") String param) {
		logger.debug("K8sFeaturesServlet::getFeatures");
		JSONObject result = new JSONObject();
		JSONArray features = new JSONArray();
		FeatureRepository fr = featuresRepo;
		
		Feature.Type type = Feature.Type.FEATURE;
		if(param != null && !param.trim().isEmpty()){
			type = Feature.Type.valueOf(param.trim().toUpperCase());
		}
		
		logger.debug("K8sFeaturesServlet::getFeatures: type = %s", type);
		try {
			for(Feature f : fr.findFeatures(type)) {
				JSONObject jf = new JSONObject();
				jf.put("name", f.getName());
				jf.put("bsn", f.getSymbolicName());
				jf.put("version", f.getVersion());
				jf.put("url", f.getDoc() != null ? f.getDoc().toString():"");
				jf.put("desc", f.getDesc() != null ? f.getDesc().toString():"");
				
				if(type == Type.ASMB){
					JSONArray asmbFeatures = new JSONArray();
					// get related features
					RequirementBuilder requirementBuilder = repos.newRequirementBuilder(IdentityNamespace.IDENTITY_NAMESPACE);
					requirementBuilder.addDirective("filter", "(" + IdentityNamespace.IDENTITY_NAMESPACE + "=" + f.getSymbolicName() + ")");
					requirementBuilder.addVersionRangeFilter(new VersionRange("["+f.getVersion()+","+f.getVersion()+"]"));
					repos.findProviders(requirementBuilder.build()).stream()
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
			return Response.ok(result.toString())
					   .header("Cache-Control", "no-cache")
					   .build();
		
		} catch(Exception e) {
			logger.warn("K8sFeaturesServlet::getFeatures", e);
			return Response.serverError().build();
		}
	}
	
	@GET
	@Path("cmd/default/obr/list")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getDefaultObr() {
		logger.debug("K8sFeaturesServlet::getDefaultObr");
		
		try {
			JSONObject result = new JSONObject();
			JSONArray obrs = new JSONArray();
			for (String obr : defaultObrUrls.split(",")) {
				obr = obr.trim();
				if (!obr.isEmpty()) {
					obrs.put(obr);
				}
			}
			
			result.put("obrs", obrs);
			result.put("localobr", localObr);
			return Response.ok(result.toString())
					   .header("Cache-Control", "no-cache")
					   .build();
		} catch(Exception e) {
			logger.warn("K8sFeaturesServlet::getDefaultObr", e);
			return Response.serverError().build();
		}
	}
	
	@GET
	@Path("cmd/obr/{param : .*}/bundles")
	public Response getBundles(@PathParam("param") String param) {
		logger.debug("K8sFeaturesServlet::getBundles");
		try {
			String obrUrlDecoded = URLDecoder.decode(param,"UTF-8");
			logger.debug("K8sFeaturesServlet::getBundles, obr = %s", obrUrlDecoded);
			URL obrUrl = new URL(obrUrlDecoded);
	        URLConnection connection = obrUrl.openConnection();
	        try(BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
	        	StringBuilder sb = new StringBuilder();
		        String inputLine;
		        while ((inputLine = in.readLine()) != null){
		        	sb.append(inputLine);
		        }
		        return Response.ok(sb.toString())
						   .build();
	        }
		} catch(IOException e) {
			logger.warn("K8sFeaturesServlet::getBundles", e);
			return Response.serverError().build();
		}
	}
	
	@GET
	@Path("cmd/bundle/{param1 : .*}/{param2 : .*}/resolution")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getBundleResolution(@Context HttpServletRequest request, @PathParam("param1") String bsn, @PathParam("param2") String version) {
		logger.debug("K8sFeaturesServlet::getBundleResolution");
		try {
			BundleRepository repo = repos;
			boolean all = request.getParameter("all") != null;
			logger.debug("K8sFeaturesServlet::getBundleResolution: bsn = %s, version = %s, all = %b", bsn, version, all);
			
			RequirementBuilder requirementBuilder = repo.newRequirementBuilder(IdentityNamespace.IDENTITY_NAMESPACE, bsn);
			if (version != null && version.length() > 0 && ! version.endsWith("none")) {
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
			
			return Response.ok(json.toString())
					   .header("Cache-Control", "no-cache")
					   .build();
		} catch(Exception e) {
			logger.warn("K8sFeaturesServlet::getBundleResolution", e);
			return Response.serverError().build();
		}
	}
	
	@GET
	@Path("cmd/bundle/{param1 : .*}/{param2 : .*}/dependent")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getDependentBundles(@Context HttpServletRequest request, @PathParam("param1") String bsn, @PathParam("param2") String version) {
		logger.debug("K8sFeaturesServlet::getDependentBundles");
		try {
			BundleRepository repo = repos;
			boolean all = request.getParameter("all") != null;
			logger.debug("K8sFeaturesServlet::getDependentBundles: bsn = %s, version = %s, all = %b", bsn, version, all);
			
			// first get matching bundle		
			RequirementBuilder requirementBuilder = repo.newRequirementBuilder(IdentityNamespace.IDENTITY_NAMESPACE, bsn);
			if (version != null && version.length() > 0 && !version.endsWith("none")) {
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
							new Package((String) rcs.getAttributes().get(OSGI_WIRING_PACKAGE), 
										 rcs.getAttributes().containsKey(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE) ? 
												 rcs.getAttributes().get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE).toString() 
												 : null)
						).collect(Collectors.toList());
				logger.debug("K8sFeaturesServlet::getDependentBundles: bsn = %s, version = %s, found exported packages: %s", bsn, version, packages);
				
				Set<Resource> dependingResources = new HashSet<Resource>();
				if(!packages.isEmpty()){
					//Retrieve all resources				
					requirementBuilder = repo.newRequirementBuilder(IdentityNamespace.IDENTITY_NAMESPACE, "*");
					requirementBuilder.addBundleIdentityFilter();
					cs = repo.findProviders(requirementBuilder.build());
					if (!cs.isEmpty()) {					
						cs.forEach(c -> {						
							Resource res = c.getResource();
							if(res.getRequirements(OSGI_WIRING_PACKAGE).stream().anyMatch(rr -> {
								try {
									String filterString = rr.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
									org.osgi.framework.Filter filter = bc.createFilter(filterString);
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
			
			return Response.ok(json.toString())
					   .header("Cache-Control", "no-cache")
					   .build();
		} catch(Exception e) {
			logger.warn("K8sFeaturesServlet::getDependentBundles", e);
			return Response.serverError().build();
		}
	}
	
	@POST
	@Path("/cmd/deploy")
	@Produces(MediaType.APPLICATION_JSON)
	public Response deployRuntime(@Context HttpServletRequest request) {
		logger.debug("K8sFeaturesServlet::deployRuntime");
		try {
			JSONObject json = getJSONContent(request);
			logger.debug("K8sFeaturesServlet::deployRuntime: got json object: %s", json);

			com.nokia.as.features.admin.k8s.Runtime runtime = new com.nokia.as.features.admin.k8s.Runtime(json);
			logger.debug("K8sFeaturesServlet::deployRuntime: created runtime %s", runtime);
			deployer.deployRuntime(runtime);
			logger.debug("K8sFeaturesServlet::deployRuntime: runtime deployed.");
			return Response.ok().build();
		} catch(Exception e) {
			logger.warn("K8sFeaturesServlet::deployRuntime", e);
			return Response.serverError().build();
		}
	}
	
	@POST
	@Path("cmd/route")
	@Produces(MediaType.APPLICATION_JSON)
	public Response createRoute(@Context HttpServletRequest request) {
		logger.debug("K8sFeaturesServlet::createRoute");
		try {
			JSONObject json = getJSONContent(request);
			logger.debug("K8sFeaturesServlet::createRoute: got json object: %s", json);

			Route route = new Route(json);
			logger.debug("K8sFeaturesServlet::createRoute: created route %s", route);
			
			deployer.createRoute(route);
			logger.debug("K8sFeaturesServlet::createRoute: route created.");
			return Response.ok().build();
		} catch(Exception e) {
			logger.warn("K8sFeaturesServlet::createRoute", e);
			return Response.serverError().build();
		}
	}
	
	@POST
	@Path("cmd/function")
	@Produces(MediaType.APPLICATION_JSON)
	public Response createFunction(@Context HttpServletRequest request) {
		logger.debug("K8sFeaturesServlet::createFunction");
		try {
			JSONObject json = getJSONContent(request);
			logger.debug("K8sFeaturesServlet::createFunction: got json object: %s", json);

			Function function = new Function(json);
			logger.debug("K8sFeaturesServlet::createFunction: created function %s", function);
			
			deployer.createFunction(function);
			logger.debug("K8sFeaturesServlet::createFunction: function created.");
			return Response.ok().build();
		} catch(Exception e) {
			logger.warn("K8sFeaturesServlet::createFunction", e);
			return Response.serverError().build();
		}
	}
	
	@POST
	@Path("/cmd/repos/reload")
	public Response reloadRepos(@Context HttpServletRequest request) {
		logger.debug("K8sFeaturesServlet::reloadRepos");
		try {
			repos.reloadLocalObr();
			List<String> obrs = getObrUrls(request);
			if( obrs != null ) {
				repos.setObrUrls(obrs);
				return Response.ok().build();
			} else {
				logger.warn("K8sFeaturesServlet::reloadRepos: no obr url in request parameters");
				return Response.status(400).build();
			}
		} catch(Exception e) {
			logger.warn("K8sFeaturesServlet::reloadRepos", e);
			return Response.serverError().build();
		}
	}
	
	@DELETE
	@Path("/cmd/undeploy")
	@Produces(MediaType.APPLICATION_JSON)
	public Response undeployRuntime(@Context HttpServletRequest request) {
		logger.debug("K8sFeaturesServlet::undeployRuntime");
		try {
			JSONObject json = getJSONContent(request);
			logger.debug("K8sFeaturesServlet::undeployRuntime: got json object: %s", json);

			String name = getJsonStringValue(json, "name");
			logger.debug("K8sFeaturesServlet::undeployRuntime: name = %s, namespace = %s", name, CURRENT_NAMESPACE);
			if (name == null) {
				logger.warn("K8sFeaturesServlet::undeployRuntime: no runtime set");
				return Response.status(400).build();
			}
			
			deployer.undeployRuntime(name + "@" + CURRENT_NAMESPACE);
			return Response.ok().build();
		} catch(Exception e) {
			logger.warn("K8sFeaturesServlet::undeployRuntime", e);
			return Response.serverError().build();
		}
	}
	
	@DELETE
	@Path("/cmd/route")
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteRoute(@Context HttpServletRequest request) {
		logger.debug("K8sFeaturesServlet::deleteRoute");
		try {
			JSONObject json = getJSONContent(request);
			logger.debug("K8sFeaturesServlet::deleteRoute: got json object: %s", json);

			String name = getJsonStringValue(json, "name");
			logger.debug("K8sFeaturesServlet::deleteRoute: name = %s, namespace = %s", name, CURRENT_NAMESPACE);
			if (name == null) {
				logger.warn("K8sFeaturesServlet::deleteRoute: no runtime set");
				return Response.status(400).build();
			}
			
			deployer.deleteRoute(name + "@" + CURRENT_NAMESPACE);
			return Response.ok().build();
		} catch(Exception e) {
			logger.warn("K8sFeaturesServlet::deleteRoute", e);
			return Response.serverError().build();
		}
	}
	
	@DELETE
	@Path("/cmd/function")
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteFunction(@Context HttpServletRequest request) {
		logger.debug("K8sFeaturesServlet::deleteFunction");
		try {
			JSONObject json = getJSONContent(request);
			logger.debug("K8sFeaturesServlet::deleteFunction: got json object: %s", json);

			String name = getJsonStringValue(json, "name");
			logger.debug("K8sFeaturesServlet::deleteFunction: name = %s, namespace = %s", name, CURRENT_NAMESPACE);
			if (name == null) {
				logger.warn("K8sFeaturesServlet::deleteFunction: no runtime set");
				return Response.status(400).build();
			}
			
			deployer.deleteFunction(name + "@" + CURRENT_NAMESPACE);
			return Response.ok().build();
		} catch(Exception e) {
			logger.warn("K8sFeaturesServlet::deleteFunction", e);
			return Response.serverError().build();
		}
	}
	
	private void fillBlueprintResources(JSONArray json, Collection<Resource> resources, String namespace, boolean verbose) throws Exception{
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
		logger.debug("getObrUrls: obrs = %s", obrs);
		
		if (obrs == null || "".equals(obrs.trim())) {
			try {
				JSONObject json = getJSONContent(req);				
				logger.debug("getObrUrls: got json object: %s", json);
				obrs = getJsonStringValue(json, "obrs");
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
	
	private JSONObject getJSONContent(final HttpServletRequest req) throws Exception {
		boolean notNull = req.getContentType() != null;
		boolean isJson = notNull && req.getContentType().trim().startsWith("application/json");
		JSONObject json = null;		
		if(isJson) {
			StringBuilder jsonContent = new StringBuilder();
			String line = null;
			BufferedReader reader = new BufferedReader(new InputStreamReader(req.getInputStream()));
			while ((line = reader.readLine()) != null) {
					jsonContent.append(line);
			}
			json = new JSONObject(jsonContent.toString());
			reader.close();
		} else {
			String jsonParam = req.getParameter("json");
			if(jsonParam != null) {
				json = new JSONObject(jsonParam);
			} else if (notNull && req.getContentType().startsWith("multipart/form-data")) {				
				req.setAttribute("org.eclipse.jetty.multipartConfig", MULTI_PART_CONFIG);
				Part filePart = req.getPart("json");
				if(filePart != null) {
					try(Scanner scan = new Scanner(filePart.getInputStream())) {
						java.util.Scanner s = scan.useDelimiter("\\A");
						jsonParam = s.hasNext() ? s.next() : "";
						json = new JSONObject(jsonParam);
					}
				}				
			}
		}
		return json;
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
}
