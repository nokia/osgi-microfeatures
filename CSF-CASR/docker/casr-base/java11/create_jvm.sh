#!/bin/bash
echo "MODULES = $MODULES"
echo "MINIMAL_JVM = $MINIMAL_JVM"

echo "jlink + jdeps"

if [ "$MINIMAL_JVM" = true ]; then
  echo 
  echo "Generating minimal JVM, this may take a couple of minutes."
  echo "jdeps might raise exceptions, this is a known issue and should not abort the generation process."
  echo 
  jlink --module-path "$JAVA_HOME"/jmods --add-modules $MODULES --compress=2 --no-header-files --no-man-pages --strip-debug --output /opt/jdk11-minimal
  #mv /tmp/jaxb* /casr/$filename/bundles
else
  echo "Create full jvm"
  jlink --module-path "$JAVA_HOME"/jmods --add-modules ALL-MODULE-PATH --compress=2 --no-header-files --no-man-pages --strip-debug --output /opt/jdk11-minimal
fi

echo "stripping..."
strip -p --strip-unneeded /opt/jdk11-minimal/lib/server/libjvm.so