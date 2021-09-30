# Introduction
The intent of this sample code is to show how to use CASR/CJDI diameter as a library from a non OSGi environment. In this context, the sample provides two applications based on SpringBoot.
- com.nokia.casr.samples.diameter.springboot.server: this is a springboot application providing a CJDI server proxylet that is defined without using OSGi.
- com.nokia.casr.samples.diameter.springboot.client: another springboot application used to send diameter requests to the diameter server. As in the first application, the code is defined without using OSGi. The application also uses springboot web starter in order to control the application using HTTP/REST. The client also shows how to do integration (junit) tests easily using spring boot test starter and CJDI diameter client API.

# Prerequisites
Maven must be installed, and the 8080 port must not be already listened.

# Architecture
SpringBoot support the concept of "starters". a SpringBoot starter allows to embed in your springboot application the necessary dependencies for a given feature by simply adding one maven dependeny in your pom (or you gradle) project. For example, if your application needs to support REST services, you would add this single dependency in your application:

```sh
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```
Similarly, to be able to use CJDI in a springboot application and embed all the necessary CASR/CJDI dependencies, you also need to declare a CJDI starter. But before declaring such starter, you must first create one. To do so, we provide a maven "archetype" which you can use to generate your own CJDI starter. 

A Maven archetype is a kind of template that is deployed to artifactory and it can be used to bootstrap a new maven (or gradle, anything actually) project from scratch.

So, you need to run the archetype command by passing some arguments which will tell what CJDI components must be included in your CJDI starter. Indeed, CJDI is component based and we don't provide a mega jar; instead you must select the features you want and a CJDI OSGi runtime is then generated with the features you have selected. For example, for the support of Diameter, you would select the following features:

* runtime.felix.embedded.log4j1: this feature regroups the dependencies for Apache Felix OSGi framework, and log4j1 support.
* ioh.mux.diameter: this feature regroups the dependencies for the CJDI Diameter IO Handler
* agent.proxylet.diameter: this feature regroups the dependencies for the CJDI Diameter Proxylet Engine.

More features could be added like for example the support of Prometheus metrics, CSF/Kafka CSF/Service Discovery (etcd4j), etc ...
For the moment, let's use the minimal features (felix +  diameter ioh + diameter proxylet engine).
So, once you have created your CJDI starter, you can then embed it in your springboot application. 

Now, how to interact with the CJDI OSGi runtime ? This is done using CJDI "OSGi bridge" service, which allows you to obtain OSGi services from springboot class loader, and/or register your proxylets from springboot to CJDI OSGi Runtime. The interface for this service looks like the following:
```java
public interface OsgiLauncher {
	public <T> ServiceRegistration<T> registerService(Class<T> service, T implementation);
	public <T> CompletableFuture<T> getService(Class<T> service);
	public <T> CompletableFuture<T> getService(Class<T> service, String filter);
	...
}
```

Now, when your springboot application defines a dependency on the CJDI springboot starter, it is then able to be injected with the OsgiLauncher "bridge" service. Here is an example of a springboot REST controler which is getting the CJDI DiameterClientFactory osgi service using the OsgiLauncher service:

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
		DiameterClientFactory factory = dfc.get(5, TimeUnit.SECONDS);
        ...
	}
}
```
# Creating a CJDI SpringBoot starter
So, to create your CJDI springboot starter, you must invoke the CJDI springboot starter archtetype. But before, you must make sure your maven settings are properly configured in order to be able to download artifacts from maven central as well as from the CSF artifactory: this artifactory provides all CJDI artifacts.
Please add this section in your ~/.m2/settings.xml; to enable mirroring of the csf-mvn-delivered repository:

```sh
<settings>
    <mirrors>
      <mirror>
	<id>csf</id>
	<name>csf delivered</name>
	<url>http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered</url>
	<mirrorOf>central</mirrorOf>
      </mirror>
    </mirrors>

</settings>
```
And now you can invoke the following maven command that will invoke the CJDI archetype to generate your own CJDI starter:

```sh
mvn archetype:generate -U  -B \
    -Dobr=http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered/com/nokia/casr/com.nokia.casr.obr/18.10.2/com.nokia.casr.obr-18.10.2.xml \
    -DarchetypeGroupId=com.nokia.casr \
    -DarchetypeArtifactId=com.nokia.as.archetype.springboot.starter \
    -DarchetypeCatalog=local \
    -DarchetypeVersion=1.0.1 \
    -DgroupId=com.nokia.casr.samples.diameter.springboot \
    -DartifactId=com.nokia.casr.samples.diameter.springboot.starter \
    -Dversion=1.0.0 \
    -Dfeatures=runtime.felix.embedded.log4j1,ioh.mux.diameter,agent.proxylet.diameter
```
So, in the above command, you have specified the G.A.V for your new starter (both client and server will depend on it), as well as the needed CJDI features:
* -DgroupId=com.nokia.casr.samples.diameter.springboot: this is the group id of the CJDI starter you are creating
* -DartifactId=com.nokia.casr.samples.diameter.springboot.starter: this is the artifactId of the CJDI starter you are creating
* -Dversion=1.0.0: this is the version of the CJDI starter you are creating
* -Dfeatures=runtime.felix.embedded.log4j1,ioh.mux.diameter,agent.proxylet.diameter: this is the CJDI minimal features for support of diameter

# Build the generated CJDI starter
now we have generated the CJDI starter, let's build it:
```sh
cd com.nokia.casr.samples.diameter.springboot.starter
mvn clean dependency:copy install
```
Notice that the 'dependency:copy' option is needed because we need to embed in the target starter jar all the CJDI dependencies.

# Build the springboot sample server
Now we have generated and built our CJDI starter, we can now build the springboot diameter server.
The server pom is already importing the CJDI starter we have generated:

```sh
		<dependency>
			<groupId>com.nokia.casr.samples.diameter.springboot</groupId>
			<artifactId>com.nokia.casr.samples.diameter.springboot.starter</artifactId>
			<version>1.0.0</version>
		</dependency>
```
So, now let's build the server:
```sh
cd com.nokia.casr.samples.diameter.springboot.server
mvn clean install
```
# Start the diameter server
The configuration is already pre configured in the ./conf directory.
So, you can now start the server like this:
```sh
cd com.nokia.casr.samples.diameter.springboot.server
java -Das.config.file.confdir=conf -jar target/com.nokia.casr.samples.diameter.springboot.server-1.0.0.jar
```
Notice the -Das.config.file.confdir that is passed as argument: it must point the CJDI configuration directory.
Internal CJDI diameter container logs are genereted in var/log/csf.group__component.instance/msg.log, but standard output is displayed on your console.

Interesting configuration files are
* conf/log4j.properties (used to configure log4j)
* conf/diameter-pxlets.xml (used to declare your diameter proxylets)
* conf/defDiameterTcpServer.txt (used to configure diameter listening addresses)

# Build the spring diameter client application
From another console, you now have to build the application that will send requests to the server.
The client application also contains an integration (junit) test which will try to send requests to the server. `So make sure the server is running before building the client.`
You can build the client like this:
```sh
cd  com.nokia.casr.samples.diameter.springboot.client
mvn -DargLine="-Das.config.file.confdir=conf" clean install
```
You should see the traces displayed by the junit integration test:

```text
2018-03-22 15:26:39.600  INFO 4144 --- [           main] c.n.c.s.d.s.client.DiameterLoaderTest    : Started DiameterLoaderTest in 6.354 seconds (JVM running for 7.297)
test: launcher=com.nokia.as.osgi.launcher.impl.OsgiLauncherImpl@5fb8dc01
DiameterLoader: starting
TestClient : connected
handleResponse : response = DiameterResponse [
        Application-Id = 0x7b
        Command=1 (1)
        Flags=-
        HopByHop Identifier (client-side) = 7340033
        HopByHop Identifier (server-side) = 7340033
        EndToEnd Identifier = -344981502
        Reception Address = /127.0.0.1:55718
        DiameterAVP [code=263, name=Session-Id, vendorId=0,flags#0=-M-,value#0:UTF8String="client.nokia.com;3730717598;0;client"]
        DiameterAVP [code=264, name=Origin-Host, vendorId=0,flags#0=-M-,value#0:Identity=testserver.nokia.com]
        DiameterAVP [code=296, name=Origin-Realm, vendorId=0,flags#0=-M-,value#0:Identity=nokia.com]
        DiameterAVP [code=260, name=Vendor-Specific-Application-Id, vendorId=0,flags#0=-M-,value#0:GroupedAVP={
          AVP#0=DiameterAVP [code=266, name=Vendor-Id, vendorId=0,flags#0=-M-,value#0:Unsigned32=10415],
          AVP#1=DiameterAVP [code=259, name=Acct-Application-Id, vendorId=0,flags#0=-M-,value#0:Unsigned32=123]}]
        DiameterAVP [code=268, name=Result-Code, vendorId=0,flags#0=-M-,value#0:Unsigned32=2001]
        DiameterAVP [code=1, name=User-Name, vendorId=0,flags#0=-M-,value#0:UTF8String="HelloWorld 1"]
        DiameterAVP [code=99, name=<???>, vendorId=10415
          flags#0=V--,value#0:OctetString(Binary)=[0x00('?'), 0x00('?'), 0x00('?'), 0x01('?')]
          flags#1=V--,value#1:OctetString(Binary)=[0x00('?'), 0x00('?'), 0x00('?'), 0x02('?')]]
]
Received 1 responses
```
# Start the diameter client application
Now you can start the diameter client application: it won't send requests until you hit "http://localhost:8080/start" (there is also a springboot web starter in the client):

```sh
cd com.nokia.casr.samples.diameter.springboot.client
java -Das.config.file.confdir=conf -jar target/com.nokia.casr.samples.diameter.springboot.client-1.0.0.jar
```
Like in the server, the CJDI internal logs are generated to var/log/csf.group__component.instance/msg.log, but standard output is displayed on your console.

# Connect to the diameter client using your brower
Now, to let the client application start loading the server, simply go to this url:
```sh
http://localhost:8080/start
```
You should now see traces displayed on both server console, as well as on client console.
And to ask the client to stop loading the server, go to this url:
```sh
http://localhost:8080/stop
```