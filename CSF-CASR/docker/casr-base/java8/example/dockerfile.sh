#!/bin/bash

### To run this script:
## ./dockerfile.sh --name=udplb --version=1.0.0 --casr=19.10.1 --features="runtime.felix;lib.log.log4j:1.0.0;config.advanced;discovery.k8s;ioh.lb.level4;metering.exporter.prometheus;ioh.server.jersey"

### Installs buildah if not present on your machine
rpm -qa | grep -qw buildah || yum install -y buildah

### Change storage driver to vfs for Jenkins
#sed -i 's/driver = "overlay"/driver = "vfs"/' /etc/containers/storage.conf

set -o errexit
set -x

### Pulls the last casr-base image
buildah pull csf-docker-delivered.repo.lab.pl.alcatel-lucent.com/casr-base:1.1.4
casr=$(buildah from csf-docker-delivered.repo.lab.pl.alcatel-lucent.com/casr-base:1.1.4)
mnt_casr=$(buildah mount $casr)

### Defines and parses command line arguments
## Available arguments:
# --name: name of the runtime (REQUIRED)
# --version: version of the runtime (REQUIRED)
# --casr: version of the OBR used to build the runtime (REQUIRED)
# --features: features to install (REQUIRED)
# --obr: artifactory repository where the obr is located. By default csf-mvn-delivered (OPTIONAL)
# --localport: if obr is 'local', indicates the port where the obr is listening (OPTIONAL)
# --logger: default logger of the runtime. By default 'rootLogger=WARN' (OPTIONAL)
CASR_REPO=csf-mvn-delivered
LOCAL_OBR_PORT=80
CASR_LOGGER="rootLogger=WARN"
for i in "$@"
do
case $i in
    -n=*|--name=*)
    INSTANCE_NAME="${i#*=}"
    shift
    ;;
    -v=*|--version=*)
    INSTANCE_VERSION="${i#*=}"
    shift
    ;;
    --casr=*)
    CASR_VERSION="${i#*=}"
    shift
    ;;
    --obr=*)
    CASR_REPO="${i#*=}"
    shift
    ;;
    --localport=*)
    LOCAL_OBR_PORT="${i#*=}"
    shift
    ;;
    -f=*|--features=*)
    FEATURES="${i#*=}"
    shift
    ;;
    -l=*|--logger=*)
    CASR_LOGGER="${i#*=}"
    shift
    ;;
    *)
    ;;
esac
done

### Setting the arguments as environment variables
buildah config --env INSTANCE_NAME=${INSTANCE_NAME} "${casr}"
buildah config --env INSTANCE_VERSION=${INSTANCE_VERSION} "${casr}"
buildah config --env CASR_VERSION=${CASR_VERSION} "${casr}"
buildah config --env CASR_REPO=${CASR_REPO} "${casr}"
buildah config --env LOCAL_OBR_PORT=${LOCAL_OBR_PORT} "${casr}"
buildah config --env FEATURES="${FEATURES}" "${casr}"
buildah config --env CASR_LOGGER="${CASR_LOGGER}" "${casr}"

### Extra docker labels
buildah config --label MAINTAINER="Nokia CASR Team" "${casr}"
buildah config --label IMAGE_NAME="casr-app-udplb" "${casr}"
buildah config --label BASE_IMAGE="csf-docker-delivered.repo.lab.pl.alcatel-lucent.com/casr-base:1.1.4" "${casr}"

### Copy your configuration files (if needed)
buildah copy --chown casr:casr "${casr}" 'conf/*' '/casr/conf/'

### Copy your application jars (if needed)
#buildah copy --chown casr:casr "${casr}" 'jars/*' '/casr/apps/'

### Define your application ports (if needed)
buildah config --port 10000 "${casr}"

### Call the runtime creation script
buildah run "${casr}" -- sh -c /casr/create_runtime.sh

### Set the working directory to the runtime
buildah config --workingdir /casr/"${INSTANCE_NAME}-${INSTANCE_VERSION}" "${casr}"

### Commit image and cleanup
buildah commit $casr casr-app-udplb:latest
buildah unmount $casr
buildah rm $casr

### Push image to local repository
buildah push localhost/casr-app-udplb:latest docker-archive:tmp-tar.tgz:casr-app-udplb:latest
docker load -i tmp-tar.tgz
rm -f tmp-tar.tgz