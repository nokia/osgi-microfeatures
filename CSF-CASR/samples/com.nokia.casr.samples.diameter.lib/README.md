# Introduction
The intent of this sample code is to show how to use CASR/CJDI diameter as a library from a non OSGi environment. In this context, the sample provides two applications which are launched from a simple main() method.
- com.nokia.casr.samples.diameter.lib.server: this is a simple application providing a CJDI server proxylet that is defined without using OSGi.
- com.nokia.casr.samples.diameter.lib.client: another application used to send diameter requests to the diameter server. As in the first application, the code is defined without using OSGi, and is started from a main() method.

# Prerequisites
Maven must be installed, and the 8080 port must not be already listened.

# Architecture
CASR support the concept of "starters" (like a SpringBoot starter), which  allows to generate a jar that includes all the necessary dependencies for a given set of CASR features. To be able to create a "starter" jar, we provide a maven "archetype" which you can use to generate your own CJDI starter jar. 

A Maven archetype is a kind of template that is deployed to artifactory and it can be used to bootstrap a new maven project from scratch.

So, you need to run the archetype command by passing some arguments which will tell what CASR features must be included in your starter. Then the archetype command will generate a new maven project with all needed dependencies generated in the project pom. In this sample code, the starter needs to contain the following features:

* runtime.felix.embedded.log4j1: this feature regroups the dependencies for Apache Felix OSGi framework, and log4j1 support.
* ioh.mux.diameter: this feature regroups the dependencies for the CJDI Diameter IO Handler
* agent.proxylet.diameter: this feature regroups the dependencies for the CJDI Diameter Proxylet Engine.

# Creating a CASR starter jar
So, before creating your starter jar, you must make sure your maven settings are properly configured in order to be able to download artifacts from maven central as well as from the CSF artifactory.
Please add this section in your ~/.m2/settings.xml; to enable mirroring of the csf-mvn-delivered repository:

```sh
<settings>
    <mirrors>
      <mirror>
	<id>csf</id>
	<name>csf delivered</name>
	<url>http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered</url>
	<mirrorOf>*</mirrorOf>
      </mirror>
    </mirrors>

</settings>
```
And now you can invoke the following maven command that will invoke the CJDI archetype to generate your own CJDI starter:

```sh
mvn archetype:generate -U  -B \
    -Dobr=http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered/com/nokia/casr/com.nokia.casr.obr/18.6.2/com.nokia.casr.obr-18.6.2.xml \
    -DarchetypeGroupId=com.nokia.casr \
    -DarchetypeArtifactId=com.nokia.as.archetype.lib.starter \
    -DarchetypeCatalog=local \
    -DarchetypeVersion=1.0.0 \
    -DgroupId=com.nokia.casr.samples.diameter.lib \
    -DartifactId=com.nokia.casr.samples.diameter.lib.starter \
    -Dversion=1.0.0 \
    -Dfeatures=runtime.felix.embedded.log4j1,ioh.mux.diameter,agent.proxylet.diameter
```
So, in the above command, you have specified the G.A.V for your new starter that will be created by the command (both the sample client and server will depend on it), as well as the needed CJDI features:
* -DgroupId=com.nokia.casr.samples.diameter.lib: this is the group id of the CJDI starter you are creating
* -DartifactId=com.nokia.casr.samples.diameter.lib.starter: this is the artifactId of the CJDI starter you are creating
* -Dversion=1.0.0: this is the version of the CJDI starter you are creating
* -Dfeatures=runtime.felix.embedded.log4j1,ioh.mux.diameter,agent.proxylet.diameter: this is the CJDI minimal features for support of diameter

# Build the generated CJDI starter
now we have generated the CJDI starter, let's build it:
```sh
cd com.nokia.casr.samples.diameter.lib.starter
mvn clean dependency:copy install
```
Notice that the 'dependency:copy' option is needed because we need to embed in the target starter jar all the CJDI dependencies.

# Build the sample server
Now we have generated and built our CJDI starter, we can now build the diameter server.
The server pom is already importing the CJDI starter we have generated:

```sh
		<dependency>
			<groupId>com.nokia.casr.samples.diameter.lib</groupId>
			<artifactId>com.nokia.casr.samples.diameter.lib.starter</artifactId>
			<version>1.0.0</version>
		</dependency>
```
So, now let's build the server:
```sh
cd com.nokia.casr.samples.diameter.lib.server
mvn clean install
```
# Start the diameter server
The configuration is already pre configured in the ./conf directory.
So, you can now start the server like this:
First, obtain all dependencies like this:
```sh
cd com.nokia.casr.samples.diameter.lib.server
mvn  dependency:copy-dependencies
```
This will generate all runtime dependencies in target/dependency/ directory.

And start the server like this:

```sh
cd com.nokia.casr.samples.diameter.lib.server
java -Das.config.file.confdir=conf -jar target/com.nokia.casr.samples.diameter.lib.server-1.0.0.jar:target/dependency/*
```
Notice the -Das.config.file.confdir that is passed as argument: it must point the CJDI configuration directory.
Internal CJDI diameter container logs are genereted in var/log/csf.group__component.instance/msg.log, but standard output is displayed on your console.

Interesting configuration files are
* conf/log4j.properties (used to configure log4j)
* conf/diameter-pxlets.xml (used to declare your diameter proxylets)
* conf/defDiameterTcpServer.txt (used to configure diameter listening addresses)

# Build the diameter client application
From another console, you now have to build the application that will send requests to the server.
You can build the client like this:
```sh
cd  com.nokia.casr.samples.diameter.lib.client
mvn clean install
```
Then, obtain all dependencies like this:
```sh
cd com.nokia.casr.samples.diameter.lib.server
mvn  dependency:copy-dependencies
```
This will generate all runtime dependencies in target/dependency/ directory.

# Start the diameter client application
Now you can start the diameter client application

```sh
cd com.nokia.casr.samples.diameter.lib.client
java -Das.config.file.confdir=conf -jar target/com.nokia.casr.samples.diameter.lib.client-1.0.0.jar:target/dependency/* com.nokia.casr.sample.diameter.lib.client.Applicion
```
Like in the server, the CJDI internal logs are generated to var/log/csf.group__component.instance/msg.log, but standard output is displayed on your console.

You should now see traces displayed on both server console, as well as on client console.

# API to register or obtain services to/from OSGi

In the com.nokia.casr.samples.diameter.lib.client and com.nokia.casr.samples.diameter.lib.server project, a  "bridge" API (which is exported by the generated starter) is used to interact with the CASR OSGi runtime from your main() method. The starter jar provdes the following interface, which allows you to obtain OSGi services from your main() class loader, and/or register your proxylets from your main() classloader to CASR OSGi Runtime. The interface for this service looks like the following:
```java
public interface OsgiLauncher {
	public <T> ServiceRegistration<T> registerService(Class<T> service, T implementation);
	public <T> CompletableFuture<T> getService(Class<T> service);
	public <T> CompletableFuture<T> getService(Class<T> service, String filter);
	...
}
```
Such Launcher API is provided in the generated Starter jar, so your application must dependen on it. Example:

```java
import com.nokia.as.osgi.launcher.OsgiLauncher;
import com.nokia.casr.samples.diameter.lib.starter.Starter;

public class Application {
	public static void main(String[] args) throws Exception {
		System.out.println("Starting Diameter Client");
		Starter starter = new Starter();
		OsgiLauncher launcher = starter.getOsgiLauncher();
		DiameterLoader loader = new DiameterLoader(launcher);
		loader.start();
		Thread.sleep(Integer.MAX_VALUE);
	}
}
```
