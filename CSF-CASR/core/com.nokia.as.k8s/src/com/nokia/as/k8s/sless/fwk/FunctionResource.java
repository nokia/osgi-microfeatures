package com.nokia.as.k8s.sless.fwk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.nokia.as.k8s.controller.CustomResource;
import com.nokia.as.k8s.controller.CustomResourceDefinition;
import com.nokia.as.k8s.controller.CustomResourceDefinition.Names;

public class FunctionResource extends CustomResource {

    public static final String KIND = "CasrFunction";
    
    public static final CustomResourceDefinition CRD =
    		new CustomResourceDefinition ()
    			.namespaced(true)
    			.group("nokia.com")
    			.version("v1beta1")
    			.names(new Names().kind(KIND).plural("casrfunctions").shortName("function").shortName("func"))
    			.build();

    public static final String SPEC_FUNCTION = "function";
    public static final String PROP_FUNCTION_LOCATIONS = "locations";
    public static final String PROP_FUNCTION_LAZY = "lazy";
    public static final String PROP_FUNCTION_TIMEOUT = "timeout"; // in seconds
    public static final String SPEC_PARAMS = "params";
    public static final String PROP_PARAMS_NAME = "name";
    public static final String PROP_PARAMS_VALUE = "value";
    
    public static class Function {
    	private List<String> locations = new ArrayList<>();
    	private Boolean lazy = true;
    	private Integer timeout = null;
    	
    	public Function lazy(Boolean lazy) {
    		this.lazy = lazy == null ? true : lazy;
    		return this;
    	}
    	
    	public boolean lazy() {
    		return this.lazy;
    	}
    	
    	public Function timeout(Integer timeout) {
    		this.timeout = timeout;
    		return this;
    	}
    	
    	public Optional<Integer> timeout() {
    		return Optional.ofNullable(this.timeout);
    	}
    	 	
    	public Function addLocation(String location) {
    		this.locations.add(location);
    		return this;
    	}
    	
    	public Function locations(List<String> locations) {
	    if (locations == null) locations = new ArrayList<> ();
    		this.locations = locations;
    		return this;
    	}
    	
    	public List<String> locations() {
	    return locations;
    	}

		@Override
		public String toString() {
			return "Function [locations=" + locations + ", lazy=" + lazy + ", timeout=" + timeout + "]";
		}
    }
    
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
    
    public final String name;
    public final String namespace;
    private Function function;
    private List<Param> params;
    
    public FunctionResource(String name, String namespace) {
		super(CRD);
    	this.name = name;
    	this.namespace = namespace;
    	this.function = new Function();
    	this.params = new ArrayList<>();
    }
    
    public static FunctionResource of(CustomResource resource) {
    	FunctionResource functionRes = new FunctionResource(resource.name(), resource.namespace());
    	
    	/* FUNCTION */
    	Map<String, Object> function = (Map<String, Object>) resource.spec().get(SPEC_FUNCTION);
    	Object timeoutObj = function.get(PROP_FUNCTION_TIMEOUT);
    	Integer timeout = timeoutObj == null ? null : (timeoutObj instanceof Integer ? (Integer) timeoutObj : ((Double) timeoutObj).intValue());
    	functionRes = 
    	functionRes.function(new Function()
    						  .lazy((Boolean) function.get(PROP_FUNCTION_LAZY))
    						  .locations((List<String>) function.get(PROP_FUNCTION_LOCATIONS))
    						  .timeout(timeout));
    	
    	/* PARAMS */
    	List<Map<String, Object>> params = (List<Map<String, Object>>) resource.spec().getOrDefault(SPEC_PARAMS, new ArrayList<> ());
    	for(Map<String, Object> p : params) {
	    functionRes = functionRes.addParam((String) p.get(PROP_PARAMS_NAME), p.get(PROP_PARAMS_VALUE));
    	};
    	
    	return functionRes.build();
    }
       
    public FunctionResource function(Function function) {
    	this.function = function;
    	return this;
    }
    
    public Function function() {
    	return function;
    }
    
    public FunctionResource params(List<Param> params) {
    	this.params = params;
    	return this;
    }
    
    public FunctionResource addParam(String name, Object value) {
    	this.params.add(new Param(name, value));
    	return this;
    }
    
    public List<Param> params() {
    	return this.params;
    }
    
    public Map<String, Object> paramsAsMap() {
    	return this.params.stream().collect(Collectors.toMap(p -> p.name, p -> p.value));
    }
    
    public FunctionResource build() {
    	apiVersion(CRD.group() + "/" + CRD.version());
		kind(CRD.names().kind());
		name(this.name);
		namespace(namespace);
		
		Map<String, Object> spec = spec();
		
		/* FUNCTION */
		Map<String, Object> function = new HashMap<>();
		function.put(PROP_FUNCTION_LOCATIONS, this.function().locations());
		function.put(PROP_FUNCTION_LAZY, this.function().lazy());
		if(this.function.timeout().isPresent())
			function.put(PROP_FUNCTION_TIMEOUT, this.function.timeout().get());
		spec.put(SPEC_FUNCTION, function);
		
		/* PARAMS */
		List<Map<String, Object>> params = new ArrayList<>();
		this.params().forEach(p -> {
			Map<String, Object> param = new HashMap<>();
			param.put(PROP_PARAMS_NAME, p.name);
			param.put(PROP_PARAMS_VALUE, p.value);
			params.add(param);
		});
		spec.put(SPEC_PARAMS, params);
		
		return this;
    }

	@Override
	public String toString() {
		return "FunctionResource [name=" + name + ", namespace=" + namespace + ", function=" + function + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, namespace);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		FunctionResource other = (FunctionResource) obj;
		return Objects.equals(name, other.name) && Objects.equals(namespace, other.namespace);
	}

}
