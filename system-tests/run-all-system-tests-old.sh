#!/bin/bash -x

#TODO autodiscovery of system tests ?

trap 'exit 1' ERR

DOCKER_IMAGE="csf-docker-delivered.repo.lab.pl.alcatel-lucent.com/build_centos_c_jdk8_mvn3:1.0.6"

run_in_docker() {
    if ! [ -x "$(command -v docker)" ]; then
    echo 'Error: docker is not installed.' >&2
    exit 1
    fi


    SYSTEMTESTS=`which $0`
    SYSTEMTESTS=`dirname $SYSTEMTESTS`
    SYSTEMTESTS=`(unset CDPATH ; cd $SYSTEMTESTS ; pwd)`

    if [ "$SYSTEMTESTS" == "" ]; then
        echo "Can't determine system-tests directory."
        exit 1
    fi

    CASR="$(dirname "$SYSTEMTESTS")"

    echo "running tests in docker"

    if [ "$useLocalObr" == true ]; then
        echo "use OBR in local m2 repository"
        docker run \
            -v $CASR:/casr \
            -v $HOME/.m2:/m2 \
            -w /casr/system-tests \
            $DOCKER_IMAGE /casr/system-tests/run-all-system-tests.sh file:///m2/repository/obr.xml

    else
        echo "OBR : $obr"
        docker run \
            -v $CASR:/casr \
            -w /casr/system-tests \
            $DOCKER_IMAGE /casr/system-tests/run-all-system-tests.sh $obr
    fi
    exit 0
}

if [ $# -lt 1 ] || [ "$1" == "-help" ] || [ "$1" == "-h" ]; then
    echo "Usage: run-all-system-tests.sh <OBR URL>/-m2 [-docker] [-local]"
    echo "Specifying -local option will run system tests that can't be run from jenkins."
    echo "Replace the OBR URL with -m2 to use your local OBR"
    echo "Specify -local to run all system tests in a docker container"
    exit 1
fi

useLocalObr=false

if [ "$1" == "-m2" ]; then
    obr=file://$HOME/.m2/repository/obr.xml
    useLocalObr=true
else
    obr=$1
fi

shift || true

runInDocker=false

if [ "$1" == "-docker" ]; then
    runInDocker=true
fi

shift || true
runLocalTests=false

if [ "$1" == "-local" ]; then 
    runLocalTests=true
fi

shift || true

if [ "$runInDocker" == true ]; then
    run_in_docker
fi

./create-system-test.sh $obr webagent "stest.web"
./run-system-test.sh $obr webagent
./create-system-test.sh $obr httpagent "stest.httpagent"
./run-system-test.sh $obr httpagent
./create-system-test.sh $obr dtls "stest.dtls"
./run-system-test.sh $obr dtls
./create-system-test.sh $obr http2 "stest.http2"
./run-system-test.sh $obr http2
./create-system-test.sh $obr jaxrs "stest.jaxrs"
./run-system-test.sh $obr jaxrs
./create-system-test.sh $obr sless.jaxrs "stest.sless.jaxrs"
./run-system-test.sh $obr sless.jaxrs
./create-system-test.sh $obr h2client "stest.h2client"
./run-system-test.sh $obr h2client
./create-system-test.sh $obr muxreactor "stest.mux-reactor"
./run-system-test.sh $obr muxreactor
./create-system-test.sh $obr cxf "stest.cxf"
./run-system-test.sh $obr cxf
./create-system-test.sh $obr csdc2 "stest.csdc2"
./run-system-test.sh $obr csdc2
./create-system-test.sh $obr etcd4j "stest.etcd4j"
./run-system-test.sh $obr etcd4j
./create-system-test.sh $obr csdc "stest.csdc"
./run-system-test.sh $obr csdc
./create-system-test.sh $obr ckaf "stest.ckaf"
./run-system-test.sh $obr ckaf

if [ "$runLocalTests" == true ]; then
    echo "Running local tests ..."
    ./create-system-test.sh $obr keycloak stest.com.nokia.as.keycloak
    ./run-system-test.sh $obr keycloak
fi

exit 0
