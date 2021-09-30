#!/bin/bash


onError() {
    echo "Something went wrong! Test aborted"
    exit 1
}

trap onError ERR

JACOCO_AGENT_URL="https://repo.lab.pl.alcatel-lucent.com/maven2/org/jacoco/org.jacoco.agent/0.8.4/org.jacoco.agent-0.8.4-runtime.jar"
JACOCO_AGENT_PATH="/tmp/jacoco-agent-stest.jar"
JAVA11_PSEUDO_RUNTIME="__java11_runtimes"
OLD_JAVA_HOME=$JAVA_HOME

print_help() {
    echo "Usage: $O [-d] [-k] [-s test-session-name] [-t test-name] <obr url> <test name>"
    echo "-k : skip runtime (re)generation"
    echo "-d : debug mode: logs and configurations are left in generated runtime so you can start them mannually"
    echo "-s [test-session-name] : only run given test session"
    echo "-t [test-name] : only run given test"
}

generateXmlPreamble() {
    local testCount=$1; shift
    local outputFile=$1; shift

    touch $outputFile
    printf "<testsuite name=\"(Scripted Tests)\" tests=\"%i\">\n" $testCount > $outputFile 
}

generateXmlTestReport() {
    local testId=$1; shift
    local testName=$1; shift
    local isTestFailure=$1; shift
    local testOutput=$1; shift
    local errorOutput=$1; shift
    local outputFile=$1; shift

    printf "    <testcase classname=\"%s\" name=\"%s\">\n" $testId $testName >> $outputFile
    if [[ $isTestFailure != true ]] && [[ $isTestFailure != "0" ]]; then
        echo "      <failure/>" >> $outputFile
    fi

    if [[ ! -z $testOutput ]]; then
        printf '        <system-out>\n<![CDATA[ %s ]]>\n        </system-out>\n' "$testOutput" >> $outputFile
    fi
    if [[ ! -z $errorOutput ]]; then
        printf "        <system-err>\n<![CDATA[ %s ]]>\n        </system-err>\n" "$errorOutput" >> $outputFile
    fi
    echo "    </testcase>" >> $outputFile
}

generateXmlTestFooter() {
    local outputFile=$1; shift

    echo "</testsuite>" >> $outputFile
}

containsElement () {
  local e match="$1"
  shift
  for e; do [[ "$e" == "$match" ]] && return 0; done
  return 1
}

run_dockerized() {
    echo "running the test dockerized"
    exit 0
}

while getopts "kdcs:t:" opt; do
  case $opt in
    k)
    keepRuntime=true
    ;;
    c)
    run_dockerized
    ;;
    d)
    debug=true
    ;;
    s)
    sessionName=$OPTARG
    ;;
    t)
    testName=$OPTARG
    ;;
    h)
    print_help
    exit 1
    ;;
    \?)
      echo "Invalid option: -$OPTARG" >&2
      ;;
  esac
done

shift $((OPTIND-1))


if [ "$1" == "m2" ]; then
    obr=file://$HOME/.m2/repository/obr.xml
else
    obr=$1
fi

name=$2

if [ $# -lt 2 ]; then
    print_help
    exit 1
fi

# determine root directory (on top of system-tests, where there is also the runtime dir)
SCRIPTDIR=`which $0`
SCRIPTDIR=`dirname $SCRIPTDIR`
SCRIPTDIR=`(unset CDPATH ; cd $SCRIPTDIR ; pwd)`

if [ "$SCRIPTDIR" == "" ]; then
    echo "Can't determine script directory."
    exit 1
fi

cd $SCRIPTDIR/..

# set TOPDIR which points to root dir (on top of runtime and scripts-dir)
export TOPDIR=`pwd`
export TESTDIR=${TOPDIR}/system-tests/$name
export AS_JUNIT4OSGI_REPORTSDIR=${TOPDIR}/system-tests/test-reports/junit4OSGi

if [[ ! -d system-tests/$name/ ]] ; then
    echo "No such directory $name, did you forget to use create-system-test.sh?"
    exit 1
fi

if [[ ! -z ${sessionName+x} ]]; then
    echo "Will only run test session named $sessionName"
fi

if [[ ! -z ${testName+x} ]]; then
    echo "Will only run test named $testName"
fi

mkdir -p $AS_JUNIT4OSGI_REPORTSDIR

cd system-tests/$name/

mkdir -p runtimes
mkdir -p logs 
runtimes=()

if [[ $keepRuntime != true ]]; then
    echo "cleaning runtime directory"
    rm -rf runtimes/*
fi

if [[ ! -f deployment.cnf ]] ; then
    echo "No deployment.cnf file found, exiting"
    exit 1
fi

#Read deployment.cnf and generate runtimes
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

    if [[ $keepRuntime = true ]]; then
        echo "skipping runtime generation"
    else
        if containsElement ${tokens[0]} ${java11_runtimes[@]} ; then 
            echo "Generating Java 11 runtime: ${tokens[0]}"
            JAVA_HOME=$JAVA11_HOME ../../runtime/create-runtime.sh $obr ${tokens[0]} 0.0.0 ${tokens[1]}
        else
            echo "Generating Java 8 runtime: ${tokens[0]}"
            JAVA_HOME=$OLD_JAVA_HOME ../../runtime/create-runtime.sh $obr ${tokens[0]} 0.0.0 ${tokens[1]}
        fi

        if [[ ! -f /tmp/runtime/${tokens[0]}-0.0.0.tgz ]]; then 
            echo "Runtime generation failed! exiting"
            exit 1
        fi
        
        cd runtimes
        tar zxvf /tmp/runtime/${tokens[0]}-0.0.0.tgz    
        mv ${tokens[0]}-0.0.0 ${tokens[0]}
        cd - > /dev/null
    fi
done < deployment.cnf


jacocoEnabled=false
testOK=true

echo "available runtimes: ${runtimes[@]}"
if [[ ! -f $JACOCO_AGENT_PATH ]] ; then
    echo "Fetching JaCoCo Agent for code coverage"
    wget -O $JACOCO_AGENT_PATH $JACOCO_AGENT_URL
    if [[ $? != 0 ]]; then 
        echo "failed to fetch jacoco agent"
        jacocoEnabled=false
    fi
else
    jacocoEnabled=true
fi

# temporarily disable jacoco for java 11
#jacocoEnabled=false

echo "cleaning logs dir"
rm -rf logs/*

#Iterate over test directories in the system-test dir
for i in *; do 
    if [[ ! -d $i ]] || [[ $i = "bundles" ]] || [[ $i = "runtimes" ]] || [[ $i = "logs" ]]; then
        continue
    fi

    if [[ ! -f $i/run.sh ]] && ! stat -t $i/*.test.sh >/dev/null 2>&1 ; then
        echo "directory $i skipped because it contains no run.sh or *.test.sh file"
        continue
    fi

    if [[ ! -z ${sessionName+x} ]] && [[ $sessionName != $i ]]; then
        echo "Skipping test session $i"
        continue
    fi

    echo "===== Starting Test Session '$i' ====="

    for runtime in "${runtimes[@]}"; do
        for j in $i/*; do
            if [[ ! -d $j ]]; then
                continue
            fi

            if [[ $runtime = $(basename $j) ]]; then
                echo "backing up configuration for runtime $runtime"
                mkdir -p /tmp/casr-stests-conf-backup/$runtime
                cp runtimes/$runtime/instance/* /tmp/casr-stests-conf-backup/$runtime
           fi
        done
    done 

    if [[ -f $i/init.sh ]] ; then 
        echo "Running init script"
        cd runtimes
        ../$i/init.sh $runtime 
        cd - > /dev/null
    fi

    activeRuntimes=()

    for runtime in "${runtimes[@]}"; do
        #iterate over runtime conf dirs in the test session directory
        for j in $i/*; do
            if [[ ! -d $j ]]; then
                continue
            fi

            if [[ $runtime = $(basename $j) ]]; then
                if [ ! -z "$(ls $i/$runtime/)" ]; then
                    echo "Installing custom configuration"
                    cp $i/$runtime/* runtimes/$runtime/instance 2> /dev/null
                else 
                    echo "No custom config to copy"
                fi
                    
                if [[ $jacocoEnabled == true ]] ; then
                    echo "JaCoCo Enabled"
                    echo " -javaagent:$JACOCO_AGENT_PATH=destfile=test.exec" >> runtimes/$runtime/instance/jvm.opt    
                fi

                activeRuntimes+=($runtime)
                if containsElement $runtime ${java11_runtimes[@]} ; then 
                    echo "starting runtime $runtime with Java 11" 
                    JAVA_HOME=$JAVA11_HOME runtimes/$runtime/start.sh
                else
                    echo "starting runtime $runtime with Java 8" 
                    JAVA_HOME=$OLD_JAVA_HOME runtimes/$runtime/start.sh
                fi

                sleep 5
            fi
        done
    done 

    if [[ -f $i/prepare.sh ]] ; then 
        echo "Running prepare script"
        cd $i
        ./prepare.sh
        cd - > /dev/null
    else
        echo "Waiting for the runtime(s) to be ready"
        sleep 10
    fi
    
    testSessionFailed=false
    singleTestSession=false

    if [[ -f $i/run.sh ]] ; then 
        cd $i
        echo "Starting test script"
        if ! ./run.sh ; then 
            testSessionFailed=true
        fi

        singleTestSession=true
        cd - > /dev/null
    else
        testSuccesses=()
        testOutput=()
        testNames=()
        errorOutput=()
        errOutputFile=$(mktemp)
        for testScript in $i/*.test.sh; do

            #testConfigFile=${testScript: : -2}cnf
            
            #if [[ -f $testConfigFile ]]; then
            #    source $testConfigFile
            #fi

            candidateTestName=$(basename $testScript .test.sh)
            

            if [[ ! -z ${testName+x} ]] && [[ $testName != $candidateTestName ]]; then
                echo "Skipping test $candidateTestName"
                continue
            else
                echo "Running test script $testScript"
            fi

            testNames+=($candidateTestName)
            testResult=true
            cd $i
            if ! testOutput+=("$(./$(basename $testScript) 2> $errOutputFile )"); then
                testResult=false
                echo "Test script exited incorrectly..."
            fi
            errorOutput+=("$(cat $errOutputFile)")
            cd - > /dev/null

            testSuccesses+=($testResult)

            if [[ $testResult != true ]] && [[ $testResult != "0" ]]; then
                testSessionFailed=true
            fi
        done
    fi 

    if [[ -f $i/destroy.sh ]] ; then 
        echo "Running destroy script"
        cd runtimes
        ../$i/destroy.sh
        cd -
    fi

    TEST_PACKAGE_NAME=""
    TEST_SUITE_NAME=""

    if [[ -f $i/test_infos ]] ; then
        source $i/test_infos
        echo "packageName: $TEST_PACKAGE_NAME"
        echo "suiteName: $TEST_SUITE_NAME"
    fi
  
    #Single test session are expected to use Junit and so skip the XML generation
    if [[ $singleTestSession != true ]] &&  [[ ${#testNames[@]} != 0 ]];  then
        echo "Generating XML report - ${#testNames[@]} test(s)"
        testOutputFile=$AS_JUNIT4OSGI_REPORTSDIR/TEST-$i.xml

        if [[ -z $TEST_PACKAGE_NAME ]] ; then
            TEST_PACKAGE_NAME="$name.stest"
        fi

        if [[ -z $TEST_SUITE_NAME ]] ; then
            TEST_SUITE_NAME=$i
        fi

        generateXmlPreamble ${#testSuccesses[@]} $testOutputFile
        for j in "${!testSuccesses[@]}"; do 
            generateXmlTestReport "$TEST_PACKAGE_NAME.$TEST_SUITE_NAME" "${testNames[$j]}" "${testSuccesses[$j]}" "${testOutput[$j]}" "${errorOutput[$j]}"  $testOutputFile
        done

        generateXmlTestFooter $testOutputFile
    fi

    if [[ $testSessionFailed == true ]]; then
        testOK=false
        echo "===== At least one test failed! see logs for runtimes: ${activeRuntimes[@]} ====="

        for runtime in "${activeRuntimes[@]}"; do 
            echo "===== runtime $runtime (msg.log) ====="
            if [ -f runtimes/$runtime/var/log/csf.runtime__component.instance/msg.log ]; then
		cat runtimes/$runtime/var/log/csf.runtime__component.instance/msg.log
	    fi
            echo "===== runtime $runtime (felix.log) ====="
	    if [ -f runtimes/$runtime/var/log/csf.runtime__component.instance/felix.log ]; then
		cat runtimes/$runtime/var/log/csf.runtime__component.instance/felix.log
	    fi
            echo
        done
    else 
        echo "===== Tests passed ====="
    fi

    for runtime in "${activeRuntimes[@]}"; do
        for j in $i/*; do
            if [[ ! -d $j ]]; then
                continue
            fi

            if [[ $runtime = $(basename $j) ]]; then
                echo "stopping runtime $runtime and recovering original configuration"
                if kill -15 $( cat runtimes/$runtime/var/tmp/pids/instance.pid ) 2> /dev/null ; then  
                    echo "Runtime killed"
                fi
                sleep 1

                if [ "$debug" != "true" ]; then
                            if rm runtimes/$runtime/instance/* ; then
                                echo "Configuration cleared"
                            fi
                fi

                echo "logs of this run will be copied to logs/$i/$runtime"
                mkdir -p logs/$i/$runtime
                cp -r runtimes/$runtime/var/log logs/$i/$runtime

                if [ "$debug" != "true" ]; then
                    if rm -rf runtimes/$runtime/var ; then
                        echo "log cleared"
                    fi
                    if cp /tmp/casr-stests-conf-backup/$runtime/* runtimes/$runtime/instance ; then
                        echo "Original config copied back" 
                    fi
                fi
           fi
        done
    done 
done

if ! $testOK; then
    echo "Some tests failed. Check the logs and the JUnit reports"
    exit 1
fi
