# Script used to dump all 3rd party dependency maven coordinates (G.A.V)

function set_java() {
    if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ];  then
        JAVA="$JAVA_HOME/bin/java"
    elif type -p java; then
        JAVA=java
    else
        echo "java not found in environment. either set JAVA_HOME or add java command in your PATH"
    fi
}

SCRIPTDIR=`which $0`
SCRIPTDIR=`dirname $SCRIPTDIR`
SCRIPTDIR=`(unset CDPATH ; cd $SCRIPTDIR ; pwd)`
BUNDLES=$SCRIPTDIR/../bundles
JARTOOL=$(find $BUNDLES -name *com.nokia.as.util.jartool*.jar|sort|tail -1)
FELIX=$(find $BUNDLES -name *org.apache.felix.main-*.jar|sort|tail -1)

set_java

$JAVA -cp $JARTOOL:$FELIX com.nokia.as.util.jartool.GetGav -b $BUNDLES $@


