# Introduction

The intent of this sample code is to show how to use CASR/CJDI radius as a library from a non OSGi environment. In this context, the sample provides a radius server applications based on SpringBoot, which provides a CJDI radius server proxylet defined without OSGi.

# Prerequisites
Maven must be installed

# Architecture
SpringBoot support the concept of "starters". a SpringBoot starter allows to embed in your springboot application the necessary dependencies for a given feature by simply adding one maven dependeny in your pom (or you gradle) project. For example, if your application needs to support REST services, you would add this single dependency in your application:

```sh
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```
Similarly, to be able to use CJDI radius in a springboot application and embed all the necessary CASR/CJDI dependencies, you also need to declare a CJDI starter. But before declaring such starter, you must first create one. To do so, we provide a maven "archetype" which you can use to generate your own CJDI radius starter. 

A Maven archetype is a kind of template that is deployed to artifactory and it can be used to bootstrap a new maven (or gradle, anything actually) project from scratch.

So, you need to run the archetype command by passing some arguments which will tell what CJDI components must be included in your CJDI starter. Indeed, CJDI is component based and we don't provide a mega jar; instead you must select the features you want and a CJDI OSGi runtime is then generated with the features you have selected. For example, for the support of Radius, you would select the following features:

* lib.log.log4j:1.0.0: this feature adds support for log4j 1.2 (use lib.log.log4j:2.0.0 for log4j 2.0 API)
* runtime.felix.embedded: this feature regroups the dependencies for Apache Felix OSGi framework
* ioh.mux.radius: this feature regroups the dependencies for the CJDI Radius IO Handler
* agent.proxylet.radius: this feature regroups the dependencies for the CJDI Radius Proxylet Engine.

More features could be added like for example the support of Prometheus metrics, CSF/Kafka CSF/Service Discovery (etcd4j), etc ...
For the moment, let's use the minimal features (felix +  radius ioh + radius proxylet engine).
So, once you have created your CJDI starter, you can then embed it in your springboot application. 

One more thing must be described before going ahead: It mus tbe explained how to interact with the CJDI OSGi runtime. This is done using CJDI "OSGi bridge" service, which allows you to obtain OSGi services from springboot class loader, and/or register your proxylets from springboot to CJDI OSGi Runtime. The interface for this service looks like the following:
```java
public interface OsgiLauncher {
	public <T> ServiceRegistration<T> registerService(Class<T> service, T implementation);
	public <T> CompletableFuture<T> getService(Class<T> service);
	public <T> CompletableFuture<T> getService(Class<T> service, String filter);
	...
}
```

Now, when your springboot application defines a dependency on the CJDI springboot starter, it is then able to be injected with the OsgiLauncher "bridge" service. Here is an example of a springboot REST controler which is getting the CJDI RadiusClientFactory osgi service using the OsgiLauncher service:

```java
@RestController
public class DiameterLoaderController {

	/**
	 * This service is the bridge between springboot world and CJDI osgi world.
	 * Using this service, you can then obtain CASR services, or register your
	 * springboot classes as osgi services.
	 */
	@Autowired
	private OsgiLauncher _launcher;
    
        @RequestMapping("/start")
	public synchronized String start() {
		CompletableFuture<DiameterClientFactory> dcf = _launcher.getService(DiameterClientFactory.class);
		RadiusClientFactory factory = dfc.get(5, TimeUnit.SECONDS);
        ...
	}
}
```
# Creating a CJDI SpringBoot starter
Now, let's  create your CJDI springboot starter. To do so, you must invoke the CJDI springboot starter archtetype:
```sh
mvn -s settings.xml archetype:generate -U  -B \
    -Dobr=http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered/com/nokia/casr/com.nokia.casr.obr/19.9.2/com.nokia.casr.obr-19.9.2.xml \
    -DarchetypeGroupId=com.nokia.casr \
    -DarchetypeArtifactId=com.nokia.as.archetype.springboot.starter \
    -DarchetypeCatalog=local \
    -DarchetypeVersion=1.0.8 \
    -DgroupId=com.nokia.casr.samples.radius.springboot \
    -DartifactId=com.nokia.casr.samples.radius.springboot.starter \
    -Dversion=1.0.0 \
    -Dfeatures=runtime.felix.embedded,ioh.mux.radius,agent.proxylet.radius,lib.log.log4j:1.0.0
```
So, in the above command, you have specified the G.A.V for your new starter as well as the needed CJDI features:
* -DgroupId=com.nokia.casr.samples.radius.springboot: this is the group id of the CJDI starter you are creating
* -DartifactId=com.nokia.casr.samples.radius.springboot.starter: this is the artifactId of the CJDI starter you are creating
* -Dversion=1.0.0: this is the version of the CJDI starter you are creating
* -Dfeatures=runtime.felix.embedded,ioh.mux.radius,agent.proxylet.radius,lib.log.log4j:1.0.0: this is the CJDI minimal features for support of radius

# Build the generated CJDI starter
now we have generated the CJDI starter, let's build it:
```sh
cd com.nokia.casr.samples.radius.springboot.starter
mvn -s ../settings.xml clean dependency:copy install
```
Notice that the 'dependency:copy' option is needed because we need to embed in the target starter jar all the CJDI dependencies.

# Build the springboot sample server
Now we have generated and built our CJDI starter, we can now build the springboot radius server.
The server pom is already importing the CJDI starter we have generated:

```sh
		<dependency>
			<groupId>com.nokia.casr.samples.radius.springboot</groupId>
			<artifactId>com.nokia.casr.samples.radius.springboot.starter</artifactId>
			<version>1.0.0</version>
		</dependency>
```
So, now let's build the server:
```sh
cd com.nokia.casr.samples.radius.springboot.server
mvn -s ../settings.xml clean install
```
# Start the diameter server
The configuration is already pre configured in the ./conf directory.
So, you can now start the server like this:
```sh
cd com.nokia.casr.samples.radius.springboot.server
java -Dlog4j.ignoreTCL=true -Dlog4j.configuration=file:conf/log4j.properties -Das.config.file.confdir=conf -jar target/com.nokia.casr.samples.radius.springboot.server-1.0.0.jar
```
Notice the following:
 * -Das.config.file.confdir that is passed as argument: it must point the CJDI configuration directory.
 * -Dlog4j.ignoreTCL: this property must be set to true in order to ignore the thread class loader when log4j is loading classes from within the embdded OSGI container
 * -Dlog4j.configuration: must point to the log4j configuration.
 
Internal CJDI diameter container logs are genereted to stdout (see conf/log4j.properties file)

Interesting configuration files are:

* conf/log4j.properties: used to configure log4j)
* conf/radius-pxlets.xml: used to declare your diameter proxylet (TestAccessRequestProxylet)
