package com.nokia.as.k8s.sless.fwk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.nokia.as.k8s.controller.CustomResource;
import com.nokia.as.k8s.controller.CustomResourceDefinition;
import com.nokia.as.k8s.controller.CustomResourceDefinition.Names;

public class RouteResource extends CustomResource {

    public static final String KIND = "CasrRoute";

    public static final CustomResourceDefinition CRD =
		new CustomResourceDefinition ()
			.namespaced(true)
			.group("nokia.com")
			.version("v1beta1")
			.names(new Names().kind(KIND).plural("casrroutes").shortName("route"))
			.build();

    public static final String SPEC_ROUTE = "route";
    public static final String PROP_ROUTE_PATH = "path";
    public static final String PROP_ROUTE_TYPE = "type";
    public static final String SPEC_FUNCTION = "function";
    public static final String PROP_FUNCTION_NAME = "name";
    public static final String SPEC_RUNTIMES = "runtimes";
    public static final String PROP_RUNTIME_NAME = "name";
    public static final String SPEC_EXEC = "exec";
    public static final String PROP_EXEC_TTL = "ttl"; // in milliseconds
    public static final String PROP_ROUTE_PARAMS = "params";
    public static final String PROP_FUNCTION_PARAMS = "params";
    public static final String PROP_PARAMS_NAME = "name";
    public static final String PROP_PARAMS_VALUE = "value";
    
    public static class Param {
		public final String name;
		public final Object value;
		
		public Param(String name, Object value) {
			this.name = name;
			this.value = value;
		}

		@java.lang.Override
		public String toString() {
			return "{" + name + " = " + value + "}";
		}
	}
    
    public static class Route {
    	public final String path;
    	public final String type;
    	private List<Param> params;
    	
    	public Route(String path, String type) {
    		this.path = path;
    		this.type = type;
    		this.params = new ArrayList<>();
    	}
    	
    	public Route params(List<Param> params) {
        	this.params = params;
        	return this;
        }
        
        public Route addParam(String name, Object value) {
        	this.params.add(new Param(name, value));
        	return this;
        }
        
        public List<Param> params() {
        	return this.params;
        }
        
        public Map<String, Object> paramsAsMap() {
        	return this.params.stream().collect(Collectors.toMap(p -> p.name, p -> p.value));
        }

		@Override
		public String toString() {
			return "Route [path=" + path + ", type=" + type + ", params=" + params + "]";
		}
    }
    
    public static class Function {
    	public final String name;
    	private List<Param> params;
    	
    	public Function(String name) {
    		this.name = name;
    		this.params = new ArrayList<>();
    	}
    	
    	public Function params(List<Param> params) {
        	this.params = params;
        	return this;
        }
        
        public Function addParam(String name, Object value) {
        	this.params.add(new Param(name, value));
        	return this;
        }
        
        public List<Param> params() {
        	return this.params;
        }
        
        public Map<String, Object> paramsAsMap() {
        	return this.params.stream().collect(Collectors.toMap(p -> p.name, p -> p.value));
        }

		@Override
		public String toString() {
			return "Function [name=" + name + ", params=" + params + "]";
		}
    }
    
    public static class Runtime {
    	public final String name;
    	
    	public Runtime(String name) {
    		this.name = name;
    	}

		@Override
		public String toString() {
			return "Runtime [name=" + name + "]";
		}
    }
    
    public static class Exec {
    	private Integer ttl;
    	
    	public Exec ttl(Integer ttl) {
    		this.ttl = ttl;
    		return this;
    	}
    	
    	public Optional<Integer> ttl() {
    		return Optional.ofNullable(ttl);
    	}
    	
		@Override
		public String toString() {
			return "Exec [ttl=" + ttl + "]";
		}
    }
    
    public final String name;
    public final String namespace;
    public final Route route;
    public final Function function;
    private List<Runtime> runtimes;
    private Exec exec;
    
    public RouteResource(String name, String namespace, Route route, Function function) {
		super(CRD);
    	this.name = name;
    	this.namespace = namespace;
    	this.route = route;
    	this.function = function;
    	this.runtimes = new ArrayList<>();
    }
    
    public static RouteResource of(CustomResource resource) {
    	/* ROUTE */
    	Map<String, Object> routeMap = (Map<String, Object>) resource.spec().get(SPEC_ROUTE);
    	Route route = new Route((String) routeMap.get(PROP_ROUTE_PATH), (String) routeMap.get(PROP_ROUTE_TYPE));
    	List<Map<String, Object>> rParams = (List<Map<String, Object>>) routeMap.getOrDefault(PROP_ROUTE_PARAMS, new ArrayList<>());
    	for(Map<String, Object> p : rParams) {
    		route = route.addParam((String) p.get(PROP_PARAMS_NAME), p.get(PROP_PARAMS_VALUE));
    	}
    	
    	/* FUNCTION */
    	Map<String, Object> functionMap = (Map<String, Object>) resource.spec().get(SPEC_FUNCTION);
    	Function function = new Function((String) functionMap.get(PROP_FUNCTION_NAME));
    	List<Map<String, Object>> fParams = (List<Map<String, Object>>) functionMap.getOrDefault(PROP_FUNCTION_PARAMS, new ArrayList<>());
    	for(Map<String, Object> p : fParams) {
    		function = function.addParam((String) p.get(PROP_PARAMS_NAME), p.get(PROP_PARAMS_VALUE));
    	}
    	
    	RouteResource routeResource = new RouteResource(resource.name(), resource.namespace(), route, function);
    	
    	/* RUNTIMES */
    	List<Map<String, Object>> runtimes = (List<Map<String, Object>>) resource.spec().getOrDefault(SPEC_RUNTIMES, new ArrayList<>());
    	for(Map<String, Object> runtime : runtimes) {
    		routeResource = routeResource.addRuntime(new Runtime((String) runtime.get(PROP_RUNTIME_NAME))); 
    	}
    	
    	/* EXEC */
    	Map<String, Object> execMap = (Map<String, Object>) resource.spec().get(SPEC_EXEC);
    	if(execMap != null) {
    		Object ttlObj = execMap.get(PROP_EXEC_TTL);
    		Integer ttl = ttlObj == null ? null : (ttlObj instanceof Integer ? (Integer) ttlObj : ((Double) ttlObj).intValue());
    		routeResource = routeResource.exec(new Exec().ttl(ttl));
    	}
    	
    	return routeResource.build();
    }
    
    public RouteResource addRuntime(Runtime runtime) {
    	this.runtimes.add(runtime);
    	return this;
    }
    
    public void runtimes(List<Runtime> runtimes) {
    	this.runtimes = runtimes;
    }
    
    public List<Runtime> runtimes() {
    	return this.runtimes;
    }
    
    public RouteResource exec(Exec exec) {
    	this.exec = exec;
    	return this;
    }
    
    public Optional<Exec> exec() {
    	return Optional.ofNullable(exec);
    }
    
    public RouteResource build() {
    	apiVersion(CRD.group() + "/" + CRD.version());
		kind(CRD.names().kind());
		name(this.name);
		namespace(namespace);
		
		Map<String, Object> spec = spec();
		
		/* ROUTE */
		Map<String, Object> route = new HashMap<>();
		route.put(PROP_ROUTE_PATH, this.route.path);
		route.put(PROP_ROUTE_TYPE, this.route.type);
		
		List<Map<String, Object>> rParams = new ArrayList<>();
		this.route.params().forEach(p -> {
			Map<String, Object> param = new HashMap<>();
			param.put(PROP_PARAMS_NAME, p.name);
			param.put(PROP_PARAMS_VALUE, p.value);
			rParams.add(param);
		});
		route.put(PROP_ROUTE_PARAMS, rParams);
		spec.put(SPEC_ROUTE, route);
		
		/* FUNCTION */
		Map<String, Object> function = new HashMap<>();
		function.put(PROP_FUNCTION_NAME, this.function.name);
		
		List<Map<String, Object>> fParams = new ArrayList<>();
		this.function.params().forEach(p -> {
			Map<String, Object> param = new HashMap<>();
			param.put(PROP_PARAMS_NAME, p.name);
			param.put(PROP_PARAMS_VALUE, p.value);
			fParams.add(param);
		});
		function.put(PROP_FUNCTION_PARAMS, fParams);
		spec.put(SPEC_FUNCTION, function);
		
		/* RUNTIMES */
		List<Map<String, Object>> runtimes = new ArrayList<>();
		this.runtimes().forEach(r -> {
			Map<String, Object> runtime = new HashMap<>();
			runtime.put(PROP_RUNTIME_NAME, r.name);
			runtimes.add(runtime);
		});
		spec.put(SPEC_RUNTIMES, runtimes);
		
		/* EXEC */
		Map<String, Object> exec = new HashMap<>();
		if(this.exec().isPresent()) {
			Exec e = this.exec().get();
			if(e.ttl().isPresent())
				exec.put(PROP_EXEC_TTL, e.ttl().get());
		}
		spec.put(SPEC_EXEC, exec);
		
		return this;
    }

	@Override
	public String toString() {
		return "RouteResource [name=" + name + ", namespace=" + namespace + ", route=" + route + ", function="
				+ function + ", runtimes=" + runtimes + ", exec=" + exec + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((namespace == null) ? 0 : namespace.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		RouteResource other = (RouteResource) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (namespace == null) {
			if (other.namespace != null)
				return false;
		} else if (!namespace.equals(other.namespace))
			return false;
		return true;
	}
}
