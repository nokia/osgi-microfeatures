# Prerequisites

 For DTLS support, this sample requires a recent version of jdk8 (>= 1.8.0_131)
 To build the source code, you need to configure your maven settings in order to use the csf-mvn-delivered repository (which also proxies maven central). For example, you can configure your $HOME/.m2/repository/settings.xml like this:
```xml
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

Also, since CASR is using OSGI, a basic understanding of OSGI is needed, please check the following [tutorial](https://confluence.app.alcatel-lucent.com/display/plateng/OSGi+presentation).

# Overview
This directory contains some sample codes using CJDI Diameter component. The sample code is provided as maven projects, which download the necessary artifacts from the csf-mvn-delivered maven repository. The sample also shows how to expose OSGi service components to JMX, using Aries JMX libraries.
In a nutshell, the samples provides:
- a `client` runtime which contains a component that is sending some diameter requests to a diameter server
- a server runtime which responds to the client requests using a diameter proxylet. You will find two kinds of servers: one simple `server` runtime containing a diameter proxylet, and a `server-spring` runtime which embeds a spring framework in the diameter proxylet bundle. The `test-spring` runtime shows how to write a spring based application that is fully embedded in an osgi bundle.

This directory contains the following maven projects:

#### ./client/

This project contains a "ClientDemo" component that is used to demonstrate the usage of the CJDI
"DiameterClient" component. The DiameterClient is then used to send diameter requests to the diameter server. The ClientDemo OSGI Component gets the DiameterClient using declararive Service @Reference annotation.

#### ./server/

This project contains a `TestServer` proxylet which is used to respond to the requests sent by the
"ClientDemo" component. The TestServer proxylet has to be defined as an OSGI service and it must be configured in the instance/diameter-pxletx.xml file.

#### ./server-spring/

Same example as the TestServer, except that the bundle containing the TestServer proxylet boots a Spring Framework in order to manage the proxylet. 
See the Boostrap.java osgi service which acts as a bridge between osgi and Spring: it initializes Spring, and then obtain a TestServer proxylet mbean from the spring context; then it registers it into the Osgi service registry in order to make it handled by the CJDI diameter container. Notice that the proxylet must also be configured in the instance/diameter-pxletx.xml file.

## JMX

Both the  `server` and `server-spring` projects shows how to simply expose some OSGi services to JMX. TO do so we are using Apache Aries JMX. [See aries tutorial]( http://aries.apache.org/modules/jmx.html). To expose some osgi services to JMX, you first need to register an MBeanServer of your choice in the OSGI service registry (see the MBeanServerComponent.java in both server and server-spring projects). Then the OSGi services must be registered with the special standard `jmx.objectname` OSGI service property that allows to specify the jmx object name. See the TestServerProxylet.java in the server project, or the Bootstrap.jave in the server-spring project.

# Building the sample code
First, build everyting (it is assumed your machine has access to the follinwg csf maven repo: http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered/).

To build, first type this command from the current directory:

```sh
mvn clean install
```

This command will generate the following artifacts:
```text
client/target/com.nokia.casr.samples.diameter.client-1.0.0-SNAPSHOT.jar
server/target/com.nokia.casr.samples.diameter.server-1.0.0-SNAPSHOT.jar
server-spring/target/com.nokia.casr.samples.diameter.server.spring-1.0.0-SNAPSHOT.jar
```

# Creating the client and server runtime
So, to generate the runtimes, please type this command:
```sh
./install/create-runtimes.sh
```
The script uses the "microfeature" tool in order to generate a runtime with a composition of multiple features (see https://confluence.app.alcatel-lucent.com/display/plateng/CASR±+MicroFeatures). Here is a description of what is doing this script:

* it will first download the casr "microfeature" tool from http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered/com/nokia/casr/microfeatures/com.nokia.casr.microfeatures.main/ location (it will take the latest available version). This tool allows to dynamically create an osgi runtime based on selected features. The features will be resolved, downloaded, and packaged by the tool into a target osgi runtime.
* The microfeature tool is then run by the script in order to generate a runtime with the following features: 
-- `runtime.felix.log4j1`: This feature provides the Felix framework with Log4j1 API
-- `agent.proxylet.diameter`: This feature provides the diameter proxylet container
-- `ioh.mux.diameter`: This feature provides the diameter io handler
-- `lib.aries.jmx`: This feature allows to expose OSGI services to JMX. 

[See the microfeature documentation for the list of all available features]( https://confluence.app.alcatel-lucent.com/display/plateng/CASR+-+MicroFeatures).

* The script will then generate three runtimes in ./runtime/client and ./runtime/server and ./runtime/server-spring
* Next, the tool will install the following artifacts from the samples:
```sh
cp client/target/com.nokia.casr.samples.diameter.client-1.0.0-SNAPSHOT.jar runtime/client/bundles/
cp server/target/com.nokia.casr.samples.diameter.server.spring-1.0.0-SNAPSHOT.jar runtime/server/bundles/
cp server-spring/target/com.nokia.casr.samples.diameter.server.spring-1.0.0-SNAPSHOT.jar runtime/server-spring/bundles/
```

* Finally, the script will override some configurations in the generated runtimes:
```sh
cp -f install/client-conf/* runtime/client/instance/
cp -f install/server-conf/* runtime/server/instance/
cp -f install/server-conf/* runtime/server-spring/instance/
```

# Starting the runtimes

To start the client from a shell console:
```sh
cd runtime/client
./start.sh -l
```
the -l option starts the client in forground and displays logs to stdout. Now you need to start either the server or the server-spring runtime. 

To start the `server` runtime from another shell console:
```sh
cd runtime/server
./start.sh -l
```

Alternatively, you can start the `server-spring` runtime (from another shell console):
```sh
cd runtime/server-spring
./start.sh -l
```

When the client and one of the servers are started, the client will periodically log this:
```text
2017-11-30 14:52:05,504 Stdout WARN  stdout  - handleResponse : response = DiameterResponse [
        Application-Id = 0x7b
        Command=1 (1)
        Flags=-
        HopByHop Identifier (client-side) = 2
        HopByHop Identifier (server-side) = 2
        EndToEnd Identifier = 452984835
        Reception Address = /127.0.0.1:52212
        DiameterAVP [code=263, name=Session-Id, vendorId=0,flags#0=-M-,value#0:UTF8String="client.nokia.com;3721038669;0;client"]
        DiameterAVP [code=264, name=Origin-Host, vendorId=0,flags#0=-M-,value#0:Identity=testserver.nokia.com]
        DiameterAVP [code=296, name=Origin-Realm, vendorId=0,flags#0=-M-,value#0:Identity=nokia.com]
        DiameterAVP [code=260, name=Vendor-Specific-Application-Id, vendorId=0,flags#0=-M-,value#0:GroupedAVP={
          AVP#0=DiameterAVP [code=266, name=Vendor-Id, vendorId=0,flags#0=-M-,value#0:Unsigned32=10415],
          AVP#1=DiameterAVP [code=259, name=Acct-Application-Id, vendorId=0,flags#0=-M-,value#0:Unsigned32=123]}]
        DiameterAVP [code=268, name=Result-Code, vendorId=0,flags#0=-M-,value#0:Unsigned32=2001]
        DiameterAVP [code=1, name=User-Name, vendorId=0,flags#0=-M-,value#0:UTF8String="HelloWorld 2"]
        DiameterAVP [code=99, name=<???>, vendorId=10415
          flags#0=V--,value#0:OctetString(Binary)=[0x00('?'), 0x00('?'), 0x00('?'), 0x01('?')]
          flags#1=V--,value#1:OctetString(Binary)=[0x00('?'), 0x00('?'), 0x00('?'), 0x02('?')]]
]
```

And the server will also log the received requests:

```text
2017-11-30 14:52:06,499 Processing-ThreadPool-3 WARN  com.nokia.casr.samples.diameter.server.spring.TestServer  - ***************** TestServer : doRequest : DiameterRequest [
        Application-Id = 0x7b
        Command=1 (1)
        Flags= REQ PXY
        HopByHop Identifier (client-side) = 3
        HopByHop Identifier (server-side) = 15728643
        EndToEnd Identifier = 452984836
        Reception Address = /127.0.0.1:3868
        DiameterAVP [code=263, name=Session-Id, vendorId=0,flags#0=-M-,value#0:UTF8String="client.nokia.com;3721038669;0;client"]
        DiameterAVP [code=260, name=Vendor-Specific-Application-Id, vendorId=0,flags#0=-M-,value#0:GroupedAVP={
          AVP#0=DiameterAVP [code=266, name=Vendor-Id, vendorId=0,flags#0=-M-,value#0:Unsigned32=10415],
          AVP#1=DiameterAVP [code=259, name=Acct-Application-Id, vendorId=0,flags#0=-M-,value#0:Unsigned32=123]}]
        DiameterAVP [code=264, name=Origin-Host, vendorId=0,flags#0=-M-,value#0:Identity=client.nokia.com]
        DiameterAVP [code=296, name=Origin-Realm, vendorId=0,flags#0=-M-,value#0:Identity=nokia.com]
        DiameterAVP [code=293, name=Destination-Host, vendorId=0,flags#0=-M-,value#0:Identity=testserver.nokia.com]
        DiameterAVP [code=283, name=Destination-Realm, vendorId=0,flags#0=-M-,value#0:Identity=nokia.com]
        Attribute[name=agent.diameter.IgnoreMayBlock, value=false]
        Attribute[name=agent.diameter.listener, value=com.nextenso.diameter.agent.peer.Peer$RequestProcessingListener@35f8a2c]
], count=3
```

# Managing the server (or server-spring) runtime using jconsole

You can connect to the server (or server-spring) runtime using jconsole. Just get the pid of the runtime (for the server or server-spring), connect jconsole to it, and check the
"com.nokia.casr.samples.diameter/Server" mbean. You will normally see the number of processed requests (see the TestServerMBean interface)

# Managing the server (or server-spring) runtime using gogo

You can also manage the server (or server-spring) runtime using Gogo Shell which allows to browse OSGI bundles and services using a "gogo shell". [Please check this tutorial about the gogo shell](https://confluence.app.alcatel-lucent.com/display/plateng/OSGi+presentation#OSGipresentation-Howtomakediagnosticswhenservicesdon'tcomeup?).

To connect to the server runtime:
```sh
cd runtime/server
./scripts/gogo.sh localhost 17001
```
(the host/port for gogo is configured in server/instance/defTcpServer.txt)

then you can type the "lb" command to list the installed OSGI bundles:
```sh
lb
START LEVEL 1
   ID|State      |Level|Name
    0|Active     |    0|System Bundle (5.6.10)|5.6.10
    1|Active     |    1|BundleInstaller (1.0.2.-RELEASE)|1.0.2.-RELEASE
    2|Active     |    1|Metering Service (V2) (1.1.5.RELEASE)|1.1.5.RELEASE
    3|Active     |    1|com.alcatel.as.service.diagnostics.impl (1.0.1.-RELEASE)|1.0.1.-RELEASE
    4|Active     |    1|Blueprint ProxyletDeployer (1.0.2.-RELEASE)|1.0.2.-RELEASE
    5|Active     |    1|Apache Felix Dependency Manager Shell (4.0.5)|4.0.5
    6|Active     |    1|New Shutdown Service (1.0.2.RELEASE)|1.0.2.RELEASE
    7|Active     |    1|MuxImpl (2.2.2.RELEASE)|2.2.2.RELEASE
    ...
```

# Dependencies

The client and server projects are using the following dependencies:
```text
org.osgi:osgi.cmpn:6.0.0
org.darkphoenixs:log4j:1.2.17
com.nokia.casr:com.nextenso.proxylet.api:1.0.1
com.nokia.cjdi:com.nextenso.proxylet.diameter:1.2.0
```

The server-spring project is using the same dependencies as above, plus some spring dependencies:
```text
commons-logging:commons-logging1.2
org.springframework:spring-context:4.2.5.RELEASE
org.springframework:spring-aop:4.2.5.RELEASE
org.springframework:spring-aspects:4.2.5.RELEASE
org.springframework:spring-beans:4.2.5.RELEASE
org.springframework:spring-context-support:4.2.5.RELEASE
org.springframework:spring-core:4.2.5.RELEASE
org.springframework:spring-expression:4.2.5.RELEASE
org.springframework:spring-instrument:4.2.5.RELEASE
org.springframework:spring-instrument-tomcat:4.2.5.RELEASE
org.springframework:spring-jdbc:4.2.5.RELEASE
org.springframework:spring-jms:4.2.5.RELEASE
org.springframework:spring-messaging:4.2.5.RELEASE
org.springframework:spring-orm:4.2.5.RELEASE
org.springframework:spring-oxm:4.2.5.RELEASE
org.springframework:spring-test:4.2.5.RELEASE
org.springframework:spring-tx:4.2.5.RELEASE
org.springframework:spring-web:4.2.5.RELEASE
org.springframework:spring-webmvc:4.2.5.RELEASE
org.springframework:spring-webmvc-portlet:4.2.5.RELEASE
org.springframework:spring-websocket:4.2.5.RELEASE
org.beanshell:bsh:2.0b4
```













