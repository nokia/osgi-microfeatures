#!/bin/bash
# Start the Felix OSGi framework ...
# set -x

# 1. remove comment lines
# 2. remove empty lines
# 3. remove leading whitespaces
# 4. join on backslash ignoring trailing whitepaces
## ---------------------------------------------------------------------------------------------------------------

function stripUnwantedChar() {
    sed -e '/^\s*#.*$/d; /^\s*$/d; s/^\s*//;' $1 | sed -e :a -e '/\\\s*$/N; s/\\\s*\n//; ta'
} 

function usage {
	echo "$0 -r <root dir> -h hostname -p platformName -g group -c component -i instance -d configDir [-pid pidfile]"
	exit 1
}

while  [ ! $# = 0 ]
do
case $1 in
    -help)
	usage
	;;
    -r)
	shift
	ASR_ROOT=$1
	;;
    -h)
	shift
	HOST=$1
	;;
    -p)
	shift
	PLATFORM=$1
	;;
    -g)
	shift
	GROUP=$1
	;;
    -c)
	shift
	COMPONENT=$1
	;;
    -i)
	shift
	INSTANCE=$1
	;;
    -d)
	shift
	CONFIG_DIR=$1
	;;
    -pid)
	shift
	PIDFILE=$1
	;;
    *)
	echo "Invalid option: $1"
	exit 1
	;;
esac
shift
done

FELIX_JAR=$(find bundles -name *org.apache.felix.main-[0-9].*.jar|sort|tail -1)
DTLS_JAR=$(find bundles -name *com.nokia.as.dtls.provider*.jar|sort|tail -1)
TLSEXPORT8_JAR=$(find bundles -name *com.nokia.as.tlsexport.patch8*.jar|sort|tail -1)
LAUNCHER_JAR=$(find bundles -name *com.alcatel.as.felix.launcher*.jar|sort|tail -1)
FELIX_PROPS=$CONFIG_DIR/felix.properties
BUNDLE_INSTALLER=$(find bundles -name *com.alcatel.as.service.bundleinstaller*.jar|sort|tail -1)

[[ -z $ASR_ROOT ]] && echo "missing parameter INSTALL_DIR (-r)" && exit 1
[[ -z $HOST ]] && HOST=`hostname`
[[ -z $PLATFORM ]] && echo "missing parameter PLATFORM (-p)" && exit 1
[[ -z $GROUP ]] && echo "missing parameter GROUP (-g)" && exit 1
[[ -z $COMPONENT ]] && echo "missing parameter COMPONENT (-c)" && exit 1
[[ -z $INSTANCE ]] && echo "missing parameter INSTANCE (-i)" && exit 1
[[ -z $CONFIG_DIR ]] && echo "missing parameter CONFIG_DIR (-d)" && exit 1
[[ -z $FELIX_JAR ]] && echo "Felix jar not found in bundles" && exit 1
[[ -z $LAUNCHER_JAR ]] && echo "FelixLauncher jar not found in bundles" && exit 1
[[ -z $FELIX_PROPS ]] && echo "Felix properties not found in $CONFIG_DIR" && exit 1
[[ -z $BUNDLE_INSTALLER ]] && echo "BundleInstaller jar not found in bundles" && exit 1

[ -e $CONFIG_DIR/jvm.opt ] && JVM_PARAMS=$(stripUnwantedChar $CONFIG_DIR/jvm.opt)
[ -e $CONFIG_DIR/user.jvm.opt ] && USER_JVM_PARAMS=$(stripUnwantedChar $CONFIG_DIR/user.jvm.opt)

#
# if CASR_LOG=xxx (with xxx=ERROR/WARN/INFO/DEBUG/TRACE), then override log4j configuration in order to configure log4j so logs are
# written to stdout.
#

function turnOffStdoutLogging() {
    log4jprop=$INSTANCE/log4j.properties
    log4j2xml=$INSTANCE/log4j2.xml
    if [ -f ${log4jprop}.$$ ]; then
	mv -f ${log4jprop}.$$ $log4jprop;
    fi    
    if [ -f ${log4j2xml}.$$ ]; then
	mv -f ${log4j2xml}.$$ $log4j2xml
    fi    
}

function turnOnLog4j1StdoutLogging() {
    arguments=$1
    logconf="$INSTANCE/log4j.properties"
    mv ${logconf} ${logconf}.$$

    # see if a rootLogger is specified

    rootLogger="log4j.rootLogger=WARN,stdout"
    for args in $arguments; do
	key=${args%%=*}
	value=${args#*=}
	if [ "$key" == "rootLogger" ]; then
	    rootLogger="log4j.rootLogger=$value,stdout"
	    break;
	fi
    done
    	
    echo "$rootLogger" > $logconf

    # parse possible configured loggers
    for args in $arguments; do
	key=${args%%=*}
	value=${args#*=}
	if [ "$key" != "rootLogger" ]; then
	    echo "log4j.logger.${key}=${value}" >> $logconf
	fi
    done

    # display stdout appenders
    echo >> $logconf
    echo "log4j.appender.stdout=org.apache.log4j.ConsoleAppender" >> $logconf
    echo "log4j.appender.stdout.layout=org.apache.log4j.PatternLayout" >> $logconf
    echo "log4j.appender.stdout.layout.ConversionPattern=%d{ISO8601} %p %c %x %t - %m%n" >> $logconf
}

function turnOnLog4j2StdoutLogging() {
    arguments=$1
    logconf="$INSTANCE/log4j2.xml"
    mv ${logconf} ${logconf}.$$

    # see if a rootLogger is specified

    rootLogger="WARN"
    for args in $arguments; do
	key=${args%%=*}
	value=${args#*=}
	if [ "$key" == "rootLogger" ]; then
	    rootLogger="$value"
	    break;
	fi
    done

    echo "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" > ${logconf}
    echo "<Configuration status=\"fatal\" monitorInterval=\"2\">" >> ${logconf}
    echo "  <Appenders>" >> ${logconf}
    echo "    <Console name=\"Console\" target=\"SYSTEM_OUT\">" >> ${logconf}
    echo "       <PatternLayout pattern=\"%d{ISO8601} %p %c %x %t - %m%n\"/>" >> ${logconf} 
    echo "    </Console>" >> ${logconf}
    echo "  </Appenders>" >> ${logconf}
    echo "  <Loggers>" >> ${logconf}
    echo "    <Root level=\"$rootLogger\">" >> ${logconf}
    echo "        <AppenderRef ref=\"Console\"/>" >> $logconf
    echo "    </Root>" >> ${logconf}

    # parse possible configured loggers
    for args in $arguments; do
	key=${args%%=*}
	value=${args#*=}
	if [ "$key" != "rootLogger" ]; then
	    echo "    <Logger name=\"$key\" level=\"$value\" additivity=\"false\">" >> $logconf
	    echo "        <AppenderRef ref=\"Console\"/>" >> $logconf
	    echo "    </Logger>" >> $logconf
	fi
    done
    echo "  </Loggers>" >> ${logconf}
    echo "</Configuration>" >> ${logconf}
}

function turnOnStdoutLogging() {
    arguments=$1
    trap "turnOffStdoutLogging" EXIT
    
    if [ -f $INSTANCE/log4j.properties ]; then
	turnOnLog4j1StdoutLogging "$arguments"
    fi
    if [ -f $INSTANCE/log4j2.xml ]; then
	turnOnLog4j2StdoutLogging "$arguments"
    fi
}

function set_java() {
    if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ];  then
        JAVA="$JAVA_HOME/bin/java"
    elif type -p java; then
        JAVA=java
    else
        echo "java not found in environment. either set JAVA_HOME or add java command in your PATH"
    fi
}

function clean_osgi_cache() {
    previous_bundle_installer_version=
    
    if [ -f $ASR_ROOT/var/tmp/bundle.installer ]; then
	previous_bundle_installer_version=`cat $ASR_ROOT/var/tmp/bundle.installer`
    fi

    if [[ "$previous_bundle_installer_version" != "$BUNDLE_INSTALLER" ]] && [[ -d $ASR_ROOT/var/tmp/osgi/ ]]; then
	echo "cleaning osgi cache"
	rm -rf $ASR_ROOT/var/tmp/osgi/
    fi

    echo $BUNDLE_INSTALLER > $ASR_ROOT/var/tmp/bundle.installer
}

set_java

if [ "$CASR_LOGSTDOUT" != "" ]; then
    turnOnStdoutLogging "$CASR_LOGSTDOUT"
fi

jdkversion=`$JAVA -version 2>&1`

# notes: 
# - we'll keep the usual INSTALL_DIR/resource in the classpath, 
#    at least as long as we run on mixed environments
# -XX:+UnlockDiagnosticVMOptions -XX:+UnsyncloadClass added for OSGi deadlock issues while loading classes
# -Djava.awt.headless=true added for java.awt.Graphics (Cedric)
# arguments "$COMPONENT ${GROUP}__${INSTANCE}" must be kept as such: there's a grep in AdminServer.sh!

if [ "${DTLS_JAR}" != "" ]; then
    BOOT_CLASSPATH=-Xbootclasspath/p:${DTLS_JAR}
    if [ "${TLSEXPORT8_JAR}" != "" ]; then
        BOOT_CLASSPATH=-Xbootclasspath/p:${TLSEXPORT8_JAR}:${DTLS_JAR}
        TLSEXPORT8_OPT='-Dcom.nokia.as.tlsexport.rfc7627_5.4=false'

	echo $jdkversion | grep '1.8.0_252-b09' | grep -i 'OpenJDK' > /dev/null
	if [[ $? -ne 0 ]] ; then
		echo "           ERROR !!! this feature requires OpenJDK Runtime Environment (build 1.8.0_252-b09)"
		exit -1
	fi

    fi
fi

echo $jdkversion|grep -e "version \"9" -e "version \"10" -e "version \"11" -e "version \"12" -e "version \"14" -e "version \"15" -e "version \"17" > /dev/null
if [ "$?" == "0" ]; then
    echo $jdkversion |grep -e "version \"17" > /dev/null
    if [ "$?" == 0 ]; then
	# under jdk17, it seems that sun.net.util package must be re exported to unnamed modules. I don't know why.
	ALL_UNNAMED="--add-opens=java.base/jdk.internal.loader=ALL-UNNAMED --add-opens=java.base/sun.net.util=ALL-UNNAMED"
    else
	ALL_UNNAMED="--add-opens=java.base/jdk.internal.loader=ALL-UNNAMED"
    fi
    CCM_JAR=$(find bundles -name *com.nokia.as.tls.ccm-[0-9].*.jar|sort|tail -1)
    if [ "$CCM_JAR" != "" ] && [ -f $CCM_JAR ]; then
	CCM="--patch-module java.base=$CCM_JAR"
    fi
    TLSEXPORT_JAR=$(find bundles -name com.nokia.as.tlsexport.patch-[0-9].*.jar|sort|tail -1)
    if [ "$TLSEXPORT_JAR" != "" ] && [ -f $TLSEXPORT_JAR ]; then
	TLSEXPORT="--patch-module java.base=$TLSEXPORT_JAR"
    fi
fi

# for java14, don't use -XX:+UnsyncloadClass, which does not exist anymore

echo $jdkversion|grep -e "version \"14" -e "version \"15" -e "version \"17" > /dev/null
if [ "$?" != "0" ]; then
    UNSYNCLOADCLASS="-XX:+UnsyncloadClass"
fi

#Try to read k8s namespace in docker secrets, and build a java param if present
if [ -f /var/run/secrets/kubernetes.io/serviceaccount/namespace ]; then
    K8S_NAMESPACE="-Dk8s.namespace=$(cat /var/run/secrets/kubernetes.io/serviceaccount/namespace)"
fi

clean_osgi_cache

CMD="$JAVA \
     ${BOOT_CLASSPATH} \
     ${ALL_UNNAMED} \
     ${CCM} \
     ${TLSEXPORT} \
     ${TLSEXPORT8_OPT} \
     -Dgroup.name=$GROUP \
     -Dcomponent.name=$COMPONENT \
     -Dinstance.name=$INSTANCE \
     -Dinstance.pid=$$ \
     -Dhost.name=$HOST \
     -Dplatform.name=$PLATFORM \
     -Dconfig.dir=$CONFIG_DIR \
     -DINSTALL_DIR=$ASR_ROOT \
     -Dbundle.installer=$BUNDLE_INSTALLER \
     -Dfelix.config.properties=file:$FELIX_PROPS \
     -DFastCacheImpl.retryConnect=true \
     ${K8S_NAMESPACE} \
     -cp $FELIX_JAR:$LAUNCHER_JAR:$CONFIG_DIR:$ASR_ROOT/resource:$INSTALL_DIR/resource \
     ${JVM_PARAMS:-} \
     ${USER_JVM_PARAMS:-} \
     -XX:+UnlockDiagnosticVMOptions ${UNSYNCLOADCLASS} \
     -Djava.awt.headless=true \
     -Djava.library.path=lib:$LD_LIBRARY_PATH \
     -Djava.util.logging.config.file=${INSTALL_DIR}/${INSTANCE}/jul.properties \
     com.alcatel.as.felix.FelixLauncher \
     $FELIX_PROPS $COMPONENT ${GROUP}__${INSTANCE} 0 `hostname` $$ localhost false"
    
echo $CMD

if [ "$PIDFILE" != "" ]; then
    output="$ASR_ROOT/var/log/csf.runtime__component.${INSTANCE}/felix.log"
    mkdir -p `dirname $output`
    nohup $CMD > $output 2>&1 &
    echo $! > $PIDFILE
    echo
    echo "check stdout logs from $ASR_ROOT/var/log/csf.runtime__component.$INSTANCE/felix.log"
    echo "check log4j logs from $ASR_ROOT/var/log/csf.runtime__component.$INSTANCE/msg.log"
else
    $CMD
fi

