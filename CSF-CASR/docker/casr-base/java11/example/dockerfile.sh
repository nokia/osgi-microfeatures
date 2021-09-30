#!/bin/bash

### To run this script:
## ./dockerfile.sh --name=udplb --version=1.0.0 --casr=19.10.1 --features="runtime.felix;lib.log.log4j:1.0.0;config.advanced;discovery.k8s;ioh.lb.level4;metering.exporter.prometheus;ioh.server.jersey"

### Installs buildah if not present on your machine
rpm -qa | grep -qw buildah || yum install -y buildah

### Change storage driver to vfs for Jenkins
#sed -i 's/driver = "overlay"/driver = "vfs"/' /etc/containers/storage.conf

set -o errexit
set -x

### This first image is a temporary image used to create the runtime and the minimal jdk11 runtime for the chosen features
### The result will be used by the following centos-nano based image that will run the runtime
### This saves about 400MB, since all unused jdk11 modules are removed

### Pulls the last casr-base image
buildah pull casr-base-11:latest
casr=$(buildah from casr-base-11:latest)
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

### Copy your configuration files (if needed)
buildah copy "${casr}" 'conf/*' '/casr/conf/'

### Copy your application jars (if needed)
#buildah copy "${casr}" 'jars/*' '/casr/apps/'

### Call the runtime creation script
### This script generates to outputs:
## - the runtime at /casr/$INSTANCE_NAME-$INSTANCE_VERSION
## - the minimal jdk11 runtime at /opt/jdk11-minimal
buildah run "${casr}" -- sh -c /casr/create_runtime.sh

### This image contains the minimal jdk11 runtime and the casr runtime only, along with the configuration files

### Pull the latest centos-nano image
buildah pull csf-docker-delivered.repo.lab.pl.alcatel-lucent.com/os_base/centos-nano:7.7-20190927
centos=$(buildah from csf-docker-delivered.repo.lab.pl.alcatel-lucent.com/os_base/centos-nano:7.7-20190927)
mnt_centos=$(buildah mount $centos)

### Copy the generated java runtime
cp -r "${mnt_casr}"/opt/jdk11-minimal "${mnt_centos}"/opt/jdk11-minimal

### Create JAVA_HOME and add it to the PATH
buildah config --env JAVA_HOME=/opt/jdk11-minimal/bin "${centos}"
buildah config --env PATH="/opt/jdk11-minimal/bin:${PATH}" "${centos}"

### Setting the arguments as environment variables in the new image as well
buildah config --env INSTANCE_NAME=${INSTANCE_NAME} "${centos}"
buildah config --env INSTANCE_VERSION=${INSTANCE_VERSION} "${centos}"
buildah config --env CASR_VERSION=${CASR_VERSION} "${centos}"
buildah config --env CASR_REPO=${CASR_REPO} "${centos}"
buildah config --env LOCAL_OBR_PORT=${LOCAL_OBR_PORT} "${centos}"
buildah config --env FEATURES="${FEATURES}" "${centos}"
buildah config --env CASR_LOGGER="${CASR_LOGGER}" "${centos}"

### Copy the generated casr
cp -r "${mnt_casr}"/casr "${mnt_centos}"/casr

### Install yum packages used by CASR at runtime
yum install -y --installroot $mnt_centos \
               --enablerepo=csf-centos7-atomic-artifactory-repo \
               --exclude=kernel* --setopt=tsflags=nodocs \
               --setopt=override_install_langs=en_US.utf8 \
    findutils hostname shadow-utils telnet which

### Create group and user casr:casr (7777:7777)
buildah run "${centos}" -- groupadd --gid 7777 -r casr
buildah run "${centos}" -- useradd --no-log-init -r --gid 7777 --uid 7777 casr
buildah run "${centos}" -- chown -R 7777:7777 /casr
buildah run "${centos}" -- mkdir /home/casr
buildah run "${centos}" -- chown -R 7777:7777 /home/casr
buildah config --user 7777 "${centos}"

### Define your application ports (if needed)
buildah config --port 10000 "${centos}"

### Set working directory and entrypoint
buildah config --workingdir /casr/"${INSTANCE_NAME}-${INSTANCE_VERSION}" "${centos}"
buildah config --entrypoint '["sh", "-c", "./start.sh -l $CASR_LOGGER"]' "${centos}"

### Commit image and cleanup
buildah commit $centos casr-app-udplb-11:latest
buildah unmount $centos
buildah unmount $casr
buildah rm $centos
buildah rm $casr

### Push image to local repository
buildah push localhost/casr-app-udplb-11:latest docker-archive:tmp-tar.tgz:casr-app-udplb-11:latest
docker load -i tmp-tar.tgz
rm -f tmp-tar.tgz