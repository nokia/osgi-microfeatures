#!/bin/bash
# Copyright 2000-2021 Nokia
#
# Licensed under the Apache License 2.0
# SPDX-License-Identifier: Apache-2.0
#


SCRIPTDIR=`which $0`
SCRIPTDIR=`dirname $SCRIPTDIR`
SCRIPTDIR=`(unset CDPATH ; cd $SCRIPTDIR ; pwd)`

TEST_DIR_README=$(cat <<'EOF'
Please add in this directory any configuration file to be deployed to your test runtime instance directory.
If there is no config file to deploy, you must create at least a dummy empty file as otherwise the empty directory
will be ignored when packaged by bnd. 
EOF
)

RUNTIMES_CNF=$(cat <<'EOF'
#Add runtime here in this format
#runtime-name=feature1,feature2,feature3,etc
#Be sure to terminate the last line with a carriage return
EOF
)

INIT_SH=$(cat <<'EOF'
#!/bin/bash

# OPTIONAL script, called before running tests and starting runtimes

EOF
)

DESTROY_SH=$(cat <<'EOF'
#!/bin/bash

# OPTIONAL script, called once call tests have been executed
EOF
)

PREPARE_SH=$(cat <<'EOF'
#!/bin/bash

# OPTIONAL script, called before running tests but AFTER starting runtimes
# can be used to ensure runtimes are ready

EOF
)

_usage(){
	echo "$0 --module-name <osgi module name> --feature-name <feature-name> [--add-runtime test-runtime1=feature1,feature2] [--with-junit]"
	echo "	the module name is the name given to the subproject and the OSGi bundle"
	echo "	the --add-runtime can be used multiple times to pass a runtime"
    echo "	definition in the form name=feature1,feature2 without any spaces."
	echo "	passing the --with-junit option will the files necessary for a simple junit test."
}

echo
echo "System test generator"
echo

if [ $# == 0 ]; then
    _usage
    exit 1
fi


allRuntimes=()
withJunit=false

while  [ ! $# = 0 ]
	do case $1 in
       --module-name)
	   shift
	   stestModuleName=$1
	   ;;
       
       --feature-name)
	   shift
	   stestFeatureName=$1
	   ;;
       
       --add-runtime)
	   shift
	   allRuntimes+=($1)
	   ;;

	   --with-junit)
	   shift
	   withJunit=true
	   ;;
   esac
   shift
done

if [ -z "$stestModuleName" ]; then
	echo "the module name cannot be empty"
	exit 1
fi

if [ -z "$stestFeatureName" ]; then
	echo "the feature name cannot be empty"
	exit 1
fi

cd $SCRIPTDIR/../core

if [ -d "$stestModuleName" ]; then
	echo "a module directory with the same name $stestModuleName already exists"
	exit 1
fi

mkdir $stestModuleName
cd $stestModuleName

cp $SCRIPTDIR/template-system-test/bnd.bnd bnd.bnd
sed -e "s/%MODULE_NAME%/$stestModuleName/g"  \
	-e "s/%FEATURE_NAME%/$stestFeatureName/g" \
	$SCRIPTDIR/template-system-test/feature.bnd > feature.bnd
 
cp $SCRIPTDIR/template-system-test/dot_classpath .classpath
sed "s/%MODULE_NAME%/$stestModuleName/g" $SCRIPTDIR/template-system-test/dot_project > .project

mkdir stest
echo "$RUNTIMES_CNF" > stest/deployment.cnf

if [ "$withJunit" = true ] ; then
	allRuntimes[0]="${allRuntimes[0]},$stestFeatureName.unit"
fi

for i in ${allRuntimes[@]}; do
	echo $i >> stest/deployment.cnf 
done

mkdir stest/test-1
echo "$INIT_SH" > stest/test-1/init.sh
echo "$PREPARE_SH" > stest/test-1/prepare.sh
echo "$DESTROY_SH" > stest/test-1/destroy.sh

for i in ${allRuntimes[@]}; do
	IFS='=' read -ra tokens <<< $i
	mkdir stest/test-1/${tokens[0]}
	echo "$TEST_DIR_README" > stest/test-1/${tokens[0]}/readme.txt
done

IFS='=' read -ra firstRuntime <<< $allRuntimes[0]

if [ "$withJunit" = true ] ; then
	srcDir=$(echo "$stestModuleName" | sed "s|\.|/|g")
	mkdir -p src/$srcDir
	sed "s/%MODULE_NAME%/$stestModuleName/g" \
		$SCRIPTDIR/template-system-test/ExampleSystemTest.java \
		> src/$srcDir/ExampleSystemTest.java
	
	sed -e "s/%MODULE_NAME%/$stestModuleName/g"  \
		-e "s/%FEATURE_NAME%/$stestFeatureName/g" \
		$SCRIPTDIR/template-system-test/feature.unit.bnd > feature.unit.bnd

	sed "s/%MODULE_NAME%/$stestModuleName/g"  \
		$SCRIPTDIR/template-system-test/unit.bnd > unit.bnd

	sed "s/%FIRST_RUNTIME%/$firstRuntime/g" \
		$SCRIPTDIR/template-system-test/run.sh > stest/test-1/run.sh
	
	chmod +x stest/test-1/run.sh
else 
	sed "s/%FIRST_RUNTIME%/$firstRuntime/g" \
 		$SCRIPTDIR/template-system-test/example.test.sh > stest/test-1/example.test.sh
	
	sed "s/%MODULE_NAME%/$stestModuleName/g" \
		$SCRIPTDIR/template-system-test/test_infos > stest/test-1/test_infos

	
	chmod +x stest/test-1/example.test.sh

fi


echo "done"
tree -a ../$stestModuleName
