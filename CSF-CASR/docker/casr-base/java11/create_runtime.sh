#!/bin/bash
echo "MICROFEATURES_URL = $MICROFEATURES_URL"
echo "MICROFEATURES_LOCAL = $MICROFEATURES_LOCAL"
echo "INSTANCE_NAME = $INSTANCE_NAME"
echo "INSTANCE_VERSION = $INSTANCE_VERSION"
echo "CASR_VERSION = $CASR_VERSION"
echo "CASR_REPO = $CASR_REPO"
echo "FEATURES = $FEATURES"
echo "OBR_URL = $OBR_URL"
echo "MINIMAL_JVM = $MINIMAL_JVM"
filename="$INSTANCE_NAME"-"$INSTANCE_VERSION"

if [ ! -d /casr/apps ]; then
  mkdir /casr/apps
fi

if [ ! -d /casr/conf ]; then
  mkdir /casr/conf
fi

if [ ! -d /tmp/conf ]; then
  mkdir /tmp/conf
fi

if [ -z "$OBR_URL" ]; then
  #Generate the correct URL to fetch the OBR
  baseObr="https://repo.lab.pl.alcatel-lucent.com"
  obrGav="casr-obr"
  if [ "$CASR_REPO" = "csf-mvn-delivered" ]; then 
      obrGav="casr"
  fi
  obrUrl=$baseObr/"$CASR_REPO"/com/nokia/${obrGav}/com.nokia.casr.obr/"$CASR_VERSION"/com.nokia.casr.obr-"$CASR_VERSION".xml

  if [ "$CASR_REPO" = "local" ]; then
      obrUrl="http://localhost:$LOCAL_OBR_PORT/obr/obr.xml"
  fi
else
  echo "Using direct URL"
  baseObr="http://repo.lab.pl.alcatel-lucent.com"
  obrUrl=$OBR_URL
fi

echo "OBR URL: " $obrUrl


#Create the runtime with microfeatures

if [ -z "$MICROFEATURES_URL" ]; then
  mfUrl="https://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered/com/nokia/casr/microfeatures/com.nokia.casr.microfeatures.main/[RELEASE]/com.nokia.casr.microfeatures.main-[RELEASE].jar"
else
  mfUrl=$MICROFEATURES_URL
fi

echo "MICROFEATURES URL: " $mfUrl

cd /tmp
if [ -z "$MICROFEATURES_LOCAL" ]; then
  echo "Using downloaded microfeatures"
  wget $mfUrl -O mf.jar
else
  echo "Using local jar"
  cp $MICROFEATURES_LOCAL /tmp/mf.jar
fi

java -Dcreate="$INSTANCE_NAME","$INSTANCE_VERSION","$FEATURES" -Dobr.remote=$obrUrl -jar mf.jar

if [ $? -ne 0 ]; then
    echo "Could not generate runtime: status=$?"
    exit 1
fi

#Unzip the runtime
cd /casr
unzip /tmp/$filename.zip
rm /tmp/$filename.zip

echo "jlink + jdeps"
if [ ! -z "$(ls /casr/apps)" ]; then
  echo "adding custom jars"
  cp /casr/apps/*.jar /casr/$filename/bundles/
fi

#mv /casr/$filename/bundles/jaxb* /tmp/
#echo $(/tmp/jdeps.sh "/casr/$filename/bundles/*.jar")

if [ "$MINIMAL_JVM" = true ]; then
  echo 
  echo "Generating minimal JVM, this may take a couple of minutes."
  echo "jdeps might raise exceptions, this is a known issue and should not abort the generation process."
  echo 
  jlink --module-path "$JAVA_HOME"/jmods --add-modules $(/tmp/jdeps.sh "/casr/$filename/bundles/*.jar") --compress=2 --no-header-files --no-man-pages --strip-debug --output /opt/jdk11-minimal
  #mv /tmp/jaxb* /casr/$filename/bundles
else
  echo "Generating full Java SE JVM"
  jlink --module-path "$JAVA_HOME"/jmods --add-modules ALL-MODULE-PATH --compress=2 --no-header-files --no-man-pages --strip-debug --output /opt/jdk11-minimal
fi

echo "stripping..."
strip -p --strip-unneeded /opt/jdk11-minimal/lib/server/libjvm.so

if [ ! -z "$(ls /casr/apps)" ]; then
  echo "removing custom jars"
  cd /casr/apps ; find . -exec rm /casr/$filename/bundles/{} \;
fi

cd /casr/$filename
ln -sf /casr/apps custo
mv /tmp/jvm.opt /casr/$filename/instance