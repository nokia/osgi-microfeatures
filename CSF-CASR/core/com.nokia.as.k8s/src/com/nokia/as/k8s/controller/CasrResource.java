package com.nokia.as.k8s.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.nokia.as.k8s.controller.CasrResource.Runtime.Build.Feature;
import com.nokia.as.k8s.controller.CustomResourceDefinition.Names;

public class CasrResource extends CustomResource {
	
	public static final String KIND = "Casr";
	
    public static final CustomResourceDefinition CRD_CASR_v1b1 =
    		new CustomResourceDefinition()
    			.namespaced(true)
    			.group("nokia.com")
    			.version("v1beta1")
    			.names(new Names().kind(KIND).plural("casrs").shortName("casr"))
    			.build();

    public static final CustomResourceDefinition CRD = CRD_CASR_v1b1;
    
	public static class Runtime {
		
		public static class Docker {
			public final String registry;
			public final String imageRepo;
			public final String imageTag;
			
			public Docker(String registry, String imageRepo, String imageTag) {
				this.registry = registry;
				this.imageRepo = imageRepo;
				this.imageTag = imageTag;
			}

			@Override
			public String toString() {
				return "Docker [registry = " + registry + ", imageRepo = " + imageRepo + ", imageTag = " + imageTag + "]";
			}
		}
		
		public static class Build {
			
			public static class Feature {
				public final String name;
				
				public Feature(String name) {
					this.name = name.replace('@', ':');
				}

				@Override
				public String toString() {
					return name;
				}
			}
			
			private List<Feature> features;
			private String version;
			private String repository;
			
			public Build() {
				this.features = new ArrayList<>();
				this.version = null;
				this.repository = null;
			}
			
			public Build addFeature(Feature feature) {
				this.features.add(feature);
				return this;
			}
			
			public Build features(List<Feature> features) {
				this.features = features;
				return this;
			}
			
			public Build version(String version) {
				this.version = version;
				return this;
			}
			
			public Build repository(String repository) {
				this.repository = repository;
				return this;
			}
			
			public List<Feature> features() { return features; }
			public Optional<String> version() { return Optional.ofNullable(version); }
			public Optional<String> repository() { return Optional.ofNullable(repository); }

			@Override
			public String toString() {
				return "Build [features=" + features + ", version=" + version + ", repository=" + repository + "]";
			}
		}
		
		private Docker docker;
		private Build build;
		private Integer replicas;
		
		public Runtime() { 
			docker = null;
			build = null;
			replicas = null;
		}
		
		public Runtime docker(Docker docker) {
			this.docker = docker;
			return this;
		}
		
		public Runtime build(Build build) {
			this.build = build;
			return this;
		}
		
		public Runtime replicas(int replicas) {
			if(replicas == 0) this.replicas = null;
			else this.replicas = replicas;
			return this;
		}
		
		public Optional<Docker> docker() { return Optional.ofNullable(docker); }
		public Optional<Build> build() { return Optional.ofNullable(build); }
		public Optional<Integer> replicas() { return Optional.ofNullable(replicas); }

		@Override
		public String toString() {
			return "Runtime [docker = " + docker + ", build = " + build + ", replicas = " + replicas + "]";
		}
	}
	
	public static class Port {
		
		public static class Ingress {
			public final String path;
			
			public Ingress(String path) {
				this.path = path;
			}

			@Override
			public String toString() {
				return "Ingress [path = " + path + "]";
			}
		}
		
		private String name;
		private int port;
		private String protocol;
		private boolean external;
		private Ingress ingress;
		
		public Port(String name, int port, String protocol, boolean external, Ingress ingress) {
			this.name = name;
			this.port = port;
			this.protocol = protocol;
			this.external = external;
			this.ingress = ingress;
		}
		
		public Port(String name, int port, String protocol) {
			this(name, port, protocol, false, null);
		}
		
		public Port external(boolean external) {
			this.external = external;
			return this;
		}
		
		public Port ingress(Ingress ingress) {
			this.ingress = ingress;
			return this;
		}
		
		public String name() { return name; }
		public int port() { return port; }
		public String protocol() { return protocol; }
		public boolean external() { return external; }
		public Optional<Ingress> ingress() { return Optional.ofNullable(ingress); }

		@Override
		public String toString() {
			return "Port [name = " + name + ", port = " + port + ", protocol = " + protocol + ", external = " + external
					+ ", ingress = " + ingress + "]";
		}
	}
	
	public static class Configuration {
		
		public static class Label {
			public final String name;
			public final String value;
			
			public Label(String name, String value) {
				this.name = name;
				this.value = value;
			}

			@java.lang.Override
			public String toString() {
				return "{" + name + " = " + value + "}";
			}
		}
		
		public static class Override {
			
			public static class Property {
				public final String key;
				public final String value;
				
				public Property(String key, String value) {
					this.key = key;
					this.value = value;
				}
				
				@java.lang.Override
				public String toString() {
					return "{" + key + " = " + value + "}";
				}
			}
			
			private boolean replace;
			private String pid;
			private List<Property> properties;
			
			public Override(boolean replace, String pid, List<Property> properties) {
				this.replace = replace;
				this.pid = pid;
				this.properties = properties;
			}
			
			public Override(boolean replace, String pid) {
				this(replace, pid, new ArrayList<>());
			}
			
			public Override addProperty(String key, String value) {
				this.properties.add(new Property(key, value));
				return this;
			}
			
			public Override properties(List<Property> properties) {
				this.properties = properties;
				return this;
			}
			
			public boolean replace() { return replace; }
			public String pid() { return pid; }
			public List<Property> properties() { return properties; }

			@java.lang.Override
			public String toString() {
				return "Override [replace = " + replace + ", pid = " + pid + ", properties = " + properties + "]";
			}
		}
		
		public static class File {
			public final String filename;
			public final String content;
			
			public File(String filename, String content) {
				this.filename = filename;
				this.content = content;
			}
			
			@java.lang.Override
			public String toString() {
				return "{" + filename + " = " + content + "}";
			}
		}
		
		public static class Environment {
			public final String name;
			public final String value;
			
			public Environment(String name, String value) {
				this.name = name;
				this.value = value;
			}
			
			@java.lang.Override
			public String toString() {
				return "{" + name + " = " + value + "}";
			}
		}
		
		public static class Prometheus {
			public final int port;
			public final String path;
			
			public Prometheus(int port, String path) {
				this.port = port;
				this.path = path;
			}

			@java.lang.Override
			public String toString() {
				return "Prometheus [port=" + port + ", path=" + path + "]";
			}
		}
		
		private List<Label> labels;
		private List<Override> overrides;
		private List<File> files;
		private List<Environment> environments;
		private Prometheus prometheus;
		private String configurationConfigMap;
		private String tlsSecret;
		
		public Configuration() {
			this.labels = new ArrayList<>();
			this.overrides = new ArrayList<>();
			this.files = new ArrayList<>();
			this.environments = new ArrayList<>();
			this.prometheus = null;
			this.configurationConfigMap = null;
			this.tlsSecret = null;
		}
		
		public Configuration addLabel(String name, String value) {
			this.labels.add(new Label(name, value));
			return this;
		}
		
		public Configuration labels(List<Label> labels) {
			this.labels = labels;
			return this;
		}
		
		public Configuration addOverride(Override override) {
			this.overrides.add(override);
			return this;
		}
		
		public Configuration overrides(List<Override> overrides) {
			this.overrides = overrides;
			return this;
		}
		
		public Configuration addFile(String filename, String content) {
			this.files.add(new File(filename, content));
			return this;
		}
		
		public Configuration files(List<File> files) {
			this.files = files;
			return this;
		}
		
		public Configuration addEnvironment(String name, String value) {
			this.environments.add(new Environment(name, value));
			return this;
		}
		
		public Configuration environments(List<Environment> environments) {
			this.environments = environments;
			return this;
		}
		
		public Configuration prometheus(Prometheus prometheus) {
			this.prometheus = prometheus;
			return this;
		}
		
		public Configuration configurationConfigMap(String configMap) {
			this.configurationConfigMap = configMap;
			return this;
		}
		
		public Configuration tlsSecret(String secret) {
			this.tlsSecret = secret;
			return this;
		}
		
		public List<Label> labels() { return labels; }
		public List<Override> overrides() { return overrides; }
		public List<File> files() { return files; }
		public List<Environment> environments() { return environments; }
		public Optional<Prometheus> prometheus() { return Optional.ofNullable(prometheus); }
		public Optional<String> configurationConfigMap() { return Optional.ofNullable(configurationConfigMap); }
		public Optional<String> tlsSecret() { return Optional.ofNullable(tlsSecret); }

		@java.lang.Override
		public String toString() {
			return "Configuration [labels = " + labels + ", overrides = " + overrides + ", files = " + files
					+ ", environments = " + environments + ", prometheus = " + prometheus + ", configurationConfigMap = "
					+ configurationConfigMap + ", tlsSecret = " + tlsSecret + "]";
		}
	}
	
	public String name;
	public String namespace;
	private Runtime runtime;
	private List<Port> ports;
	private Configuration configuration;
	
	private boolean deployment;
	
	public CasrResource() {
		super(CRD);
	}
	
	public CasrResource(String name, String namespace) {
		super(CRD);
		this.name = name;
		this.namespace = namespace;
		deployment = true;
		runtime = new Runtime();
		ports = new ArrayList<>();
		configuration = new Configuration();
	}
	
	public CasrResource runtime(Runtime runtime) {
		this.runtime = runtime;
		return this;
	}
	
	public CasrResource addPort(Port port) {
		this.ports.add(port);
		if(port.external()) deployment = false;
		return this;
	}
	
	public CasrResource ports(List<Port> ports) {
		this.ports = ports;
		boolean external = ports.stream().map(Port::external).reduce(false, Boolean::logicalOr);
		if(external) deployment = false;
		return this;
	}
	
	public CasrResource configuration(Configuration configuration) {
		this.configuration = configuration;
		return this;
	}
	
	public CasrResource build() {
		apiVersion(CRD.group() + "/" + CRD.version());
		kind(CRD.names().kind());
		name(this.name);
		namespace(this.namespace);
		
		Map<String, Object> spec = spec();
		
		/* RUNTIME */
		Map<String, Object> runtime = new HashMap<>();
		
		/** BUILD **/
		this.runtime().build().ifPresent(b -> {
			Map<String, Object> build = new HashMap<>();
			build.put("features", b.features.stream().map(Feature::toString).collect(Collectors.toList()));
			b.version().ifPresent(v -> build.put("version", v));
			b.repository().ifPresent(r -> build.put("repository", r));
			runtime.put("build", build);
		});
		
		/** DOCKER **/
		this.runtime().docker().ifPresent(d -> {
			Map<String, Object> docker = new HashMap<>();
			docker.put("registry", d.registry);
			docker.put("imageRepo", d.imageRepo);
			docker.put("imageTag", d.imageTag);
			runtime.put("docker", docker);
		});
		
		/** REPLICAS **/
		this.runtime().replicas().ifPresent(r -> runtime.put("replicas", r));
		
		spec.put("runtime", runtime);
		
		/* PORTS */
		List<Map<String, Object>> ports = new ArrayList<>();
		this.ports().forEach(p -> {
			Map<String, Object> port = new HashMap<>();
			port.put("name", p.name());
			port.put("port", p.port());
			port.put("protocol", p.protocol());
			if(p.external()) port.put("external", p.external());
			
			/** INGRESS **/
			p.ingress().ifPresent(i -> {
				Map<String, Object> ingress = new HashMap<>();
				ingress.put("path", i.path);
				port.put("ingress", ingress);
			});
			ports.add(port);
		});
		
		spec.put("ports", ports);
		
		/* CONFIGURATION */
		Map<String, Object> configuration = new HashMap<>();
		
		/** LABELS **/
		List<Map<String, Object>> labels = new ArrayList<>();
		this.configuration().labels().forEach(l -> {
			Map<String, Object> label = new HashMap<>();
			label.put("name", l.name);
			label.put("value", l.value);
			labels.add(label);
		});
		configuration.put("labels", labels);
		
		/** OVERRIDE **/
		List<Map<String, Object>> overrides = new ArrayList<>();
		this.configuration().overrides().forEach(o -> {
			Map<String, Object> override = new HashMap<>();
			override.put("pid", o.pid());
			override.put("replace", o.replace());
			
			List<Map<String, Object>> properties = new ArrayList<>();
			o.properties().forEach(p -> {
				Map<String, Object> property = new HashMap<>();
				property.put("name", p.key);
				property.put("value", p.value);
				properties.add(property);
			});
			override.put("props", properties);
			overrides.add(override);
		});
		configuration.put("override", overrides);
		
		/** FILES **/
		List<Map<String, Object>> files = new ArrayList<>();
		this.configuration().files().forEach(f -> {
			Map<String, Object> file = new HashMap<>();
			file.put("name", f.filename);
			file.put("content", f.content);
			files.add(file);
		});
		configuration.put("files", files);
		
		/** ENVIRONMENT **/
		List<Map<String, Object>> environments = new ArrayList<>();
		this.configuration().environments().forEach(e -> {
			Map<String, Object> environment = new HashMap<>();
			environment.put("name", e.name);
			environment.put("value", e.value);
			environments.add(environment);
		});
		configuration.put("env", environments);
		
		/** PROMETHEUS **/
		this.configuration().prometheus().ifPresent(p -> {
			Map<String, Object> prometheus = new HashMap<>();
			prometheus.put("port", p.port);
			prometheus.put("path", p.path);
			configuration.put("prometheus", prometheus);
		});
		
		/** CONFIGURATIONCONFIGMAP **/
		this.configuration().configurationConfigMap().ifPresent(c -> configuration.put("configurationConfigMap", c));
		
		/** TLSSECRET **/
		this.configuration().tlsSecret().ifPresent(t -> configuration.put("tlsSecret", t));
		
		spec.put("configuration", configuration);
		return this;
	}

	//public String name() { return name; }
	public Runtime runtime() { return runtime; }
	public List<Port> ports() { return ports; }
	public Configuration configuration() { return configuration; }
	
	public boolean isDeployment() {
		return this.deployment;
	}

	@Override
	public String toString() {
		return "CasrResource [name = " + name + ", namespace = " + namespace + ", runtime = " + runtime + ", ports = " + ports
				+ ", configuration = " + configuration + "]";
	}

	/*
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((namespace == null) ? 0 : namespace.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if(o == null) return false;
		if(!(o instanceof CasrResource)) return false;
		
		CasrResource other = (CasrResource) o;
		return this.name().equals(other.name()) && 
			    this.namespace().equals(other.namespace());
	}
	*/
	
	
	
}
