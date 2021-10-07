#!/bin/bash

onError() {
    echo "Something went wrong! Test aborted"
    exit 1
}

trap onError ERR

DOCKER_IMAGE="csf-docker-delivered.repo.lab.pl.alcatel-lucent.com/build_centos_c_jdk8_mvn3:1.0.6"

SYSTEMTESTS=`which $0`
SYSTEMTESTS=`dirname $SYSTEMTESTS`
SYSTEMTESTS=`(unset CDPATH ; cd $SYSTEMTESTS ; pwd)`
JAVA11_PSEUDO_RUNTIME="__java11_runtimes"

if [ "$SYSTEMTESTS" == "" ]; then
    echo "Can't determine system-tests directory."
    exit 1
fi

cd $SYSTEMTESTS/..

# set TOPDIR which points to root dir (on top of runtime and scripts-dir)
export TOPDIR=`pwd`
export RUNTIMEDIR=${TOPDIR}/runtime
cd -

containsElement () {
  local e match="$1"
  shift
  for e; do [[ "$e" == "$match" ]] && return 0; done
  return 1
}

run_in_docker() {
    if ! [ -x "$(command -v docker)" ]; then
    echo 'Error: docker is not installed.' >&2
    exit 1
    fi

    CASR="$(dirname "$SYSTEMTESTS")"

    echo "running tests in docker"
    docker pull $DOCKER_IMAGE

    if [ "$useLocalObr" == true ]; then
        echo "use OBR in local m2 repository"
        docker run \
            -e USER=$USER \
            -v $CASR:/casr \
            -v $HOME/.m2:/m2 \
            -w /casr/system-tests \
            -v /etc/passwd:/etc/passwd \
            $DOCKER_IMAGE /casr/system-tests/run-all-system-tests.sh file:///m2/repository/obr.xml

    else
        echo "OBR : $obr"
        docker run \
            -e USER=$USER \
            -v /etc/passwd:/etc/passwd \
            -v $CASR:/casr \
            -w /casr/system-tests \
            $DOCKER_IMAGE /casr/system-tests/run-all-system-tests.sh $obr
    fi
    exit 0
}

generate_all_runtimes() {
    local stests=("$@")

    echo "Collecting runtimes to generate"
    echo $stests
    declare -A stestRuntimes #[testName.runtimeName => finalPath]
    java11BulkGenArg="$obr "
    bulkGenArg="$obr "
    for stest in ${stests[@]}; do
        echo "doing $stest"
        runtimes=()
        java8Runtimes=()
        java11Runtimes=()
        
        while read line; do
            if [[ $line = \#* ]] || [[ -z $line ]] ; then
                continue
            fi
            IFS='=' read -r -a tokens <<< "$line"

            if [[ ${tokens[0]} = $JAVA11_PSEUDO_RUNTIME ]] ; then
                IFS=',' read -r -a java11_runtimes <<< "${tokens[1]}"
                echo "java 11 runtimes : ${java11_runtimes[@]}"
                continue
            else 
                runtimes+=(${tokens[0]})
            fi

            stestRuntimes[$stest.${tokens[0]}]+="$SYSTEMTESTS/$stest/runtimes/${tokens[0]}"

            if containsElement ${tokens[0]} ${java11_runtimes[@]} ; then 
                java11BulkGenArg="${java11BulkGenArg}$stest.${tokens[0]} 0.0.0 "
                java11BulkGenArg="${java11BulkGenArg} $(echo ${tokens[1]} |  tr ',' ' '),"
            else
                bulkGenArg="${bulkGenArg}$stest.${tokens[0]} 0.0.0 "
                bulkGenArg="${bulkGenArg} $(echo ${tokens[1]} |  tr ',' ' '),"
            fi
        
        done < $SYSTEMTESTS/$stest/deployment.cnf

        mkdir $stest/runtimes
    done

    echo $java11BulkGenArg
    echo $bulkGenArg
    echo "Generating Java 8 runtimes..."
    $RUNTIMEDIR/create-multiple-runtimes.sh $bulkGenArg

    echo "Generating Java 11 runtimes..."
    JAVA_HOME=$JAVA11_HOME $RUNTIMEDIR/create-multiple-runtimes.sh $java11BulkGenArg

    echo "unpacking..."
    for K in "${!stestRuntimes[@]}";
        do echo $K --- ${stestRuntimes[$K]}; 
        unzip -q /tmp/$K-0.0.0.zip  -x / -d /tmp
        rm /tmp/$K-0.0.0.zip
        mv /tmp/$K-0.0.0 ${stestRuntimes[$K]}
    done

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


if [ ! -f $SYSTEMTESTS/system-test-list.txt ]; then
    echo "system-test-list.txt not found in the system test directory"
    exit 1
fi

if [ "$runLocalTests" == true ]; then
    if [ ! -f $SYSTEMTESTS/localonly-system-test-list.txt ]; then
        echo "localonly-system-test-list.txt not found in the system test directory"
        exit 1
    fi
    
    effectiveStestList=$(mktemp /tmp/stest-list.XXXXXXXX)
    cat $SYSTEMTESTS/system-test-list.txt $SYSTEMTESTS/localonly-system-test-list.txt > $effectiveStestList
else
    effectiveStestList=$SYSTEMTESTS/system-test-list.txt
fi

cat $effectiveStestList

echo "instanciating system tests..."
$SYSTEMTESTS/create-system-test.sh $obr $effectiveStestList

stests=()
while IFS= read -r line
do
    if [[ "$line" = \#* ]] || [[ -z "$line" ]] ; then
        continue
    fi
    stests+=( $line )
done < "$effectiveStestList"

echo "generating runtimes for system tests..."
generate_all_runtimes ${stests[@]}

for stest in ${stests[@]}; do
    echo "running $stest"
    $SYSTEMTESTS/run-system-test.sh -k $obr $stest
done

echo "done!"

exit 0
