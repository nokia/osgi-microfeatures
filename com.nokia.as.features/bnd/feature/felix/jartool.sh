#!/bin/bash

# helper that can be used when looking for resources from bundles.

function set_java() {
    if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ];  then
        JAVA="$JAVA_HOME/bin/java"
    elif type -p java; then
        JAVA=java
    else
        echo "java not found in environment. either set JAVA_HOME or add java command in your PATH"
    fi
}

JARTOOL=$(find bundles -name *com.nokia.as.util.jartool*.jar|sort|tail -1)
set_java
$JAVA -jar $JARTOOL $@
