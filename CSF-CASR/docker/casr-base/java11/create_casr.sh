#!/bin/bash
echo "INSTANCE_NAME = $INSTANCE_NAME"
echo "INSTANCE_VERSION = $INSTANCE_VERSION"
echo "CASR_VERSION = $CASR_VERSION"
echo "CASR_REPO = $CASR_REPO"
echo "FEATURES = $FEATURES"
echo "OBR_URL = $OBR_URL"
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

cd /casr/$filename
ln -sf /casr/apps custo
mv /tmp/jvm.opt /casr/$filename/instance