# Nokia microfeatures project

This project provides some java components used as the underlying runtime for other Nokia assets such as Diameter, or Radius containers.
All is based on OSGI [Apache Felix](http://felix.apache.org), and on a proprietary "microfeatures" installer tool allows to create an OSGI runtime, based on generic "microfeatures".

Generic "microfeatures" provides a coherent set of OSGi bundles, when combined, providing some given functions. The OSGI resolver service is used to calculate dependencies in order to generate a consistent set of resolvable bundles.

These MicroFeatures are currently focusing on various domains, for example:

 - protocol support : HTTP, HTTP2, Diameter, Radius
 - OAM : metrics using a proprietary metering service, or distributed tracing using jaeger
 - load balancing functions
 - REST services using jersey

# To build the binaries:

First, create the following ~/.gradle/gradle.properties file with the following content:

```
org.gradle.daemon=false
org.gradle.configureondemand=true
org.gradle.parallel=false
org.gradle.jvmargs=-Xmx3g -Xms3g -XX:MaxMetaspaceSize=1024m -Dfile.encoding=UTF-8
#systemProp.http.proxyHost=hostname
#systemProp.http.proxyPort=8080
#systemProp.http.proxyUser=defusername
#systemProp.http.proxyPassword=xxx
```
Use 3 GB at minimum for jvmargs, or more. Optionaly configure proxy settings.

Now, configure a jdk8 path:

```
export PATH=<path-to-jdk8/bin>:$PATH
export JAVA_HOME=<path-to-jdk8>
```

and build everything:

```
./gradlew jar
./gradlew com.nokia.as.microfeatures.admin:export.launch
./scripts/create-obr-m2.sh
```

(the last command creates an OSGi bundle repository to $HOME/.m2/repository/obr.xml)

# Example:

To create an osgi runtime, you can use the com.nokia.as.microfeatures.admin/generated/distributions/executable/launch.jar standalone jar, like this:

```
java -Dobr=file://$HOME/.m2/repository/obr.xml -Dcreate=myruntime,1.0.0,runtime.felix,ioh.mux.diameter,agent.proxylet.diameter -jar com.nokia.as.microfeatures.admin/generated/distributions/executable/launch.jar
```

The above command will generate a zip file, containing a diameter load balancer, as well as a local diameter proxylet container.
unzip it and start the runtime:

TODO: describe the diameter load balancer, and the diameter proxylet container

```
unzip myruntime-1.0.0.zip
cd myruntime-1.0.0
./start.sh
```
to enable gogo, edit instance/defTcpServer.txt and uncomment the last "gogo-bash" tcp processor:

```
<servers>
  <!-- Uncomment the following server to activate the OSGi remote shell server -->
  <!--server ip="127.0.0.1" port="17000" processor="gogo.shell" name="gogo">
      <property name="read.timeout">1000000</property>
  </server-->

  <!-- Uncomment the following server to activate the OSGi remote bash-like shell server -->
  <server ip="127.0.0.1" port="17001" processor="gogo.client" name="gogo-bash">
      <property name="read.timeout">1000000</property>
  </server>
</servers>
```

Then connect to the osgi framework using this script:

```
./scripts/gogo.sh
```

(you can type some gogo commands like "lb", help", etc ...)

To stop the container:

```
./stop.sh
```
# Microfeatures list

TODO: describe all available microfeatures

# License

Copyright 2021 Nokia

Licensed under the Apache License 2.0
SPDX-License-Identifier: Apache-2.0
