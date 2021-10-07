#!/bin/bash

SCRIPTDIR=`which $0`
SCRIPTDIR=`dirname $SCRIPTDIR`
SCRIPTDIR=`(unset CDPATH ; cd $SCRIPTDIR ; pwd)`
DIR="."

# type of project to generate (can be ds, dm, dm.activator, or service)
TYPE=

function usage {
    CMD=`basename $0`
    echo
    echo "Usage:"
    echo "$CMD -p <project name> [-d <project top dir>] [ -t <type>]"
    echo
    echo "OPTIONS:"
    echo "	-p project name: the project name (dot seprated)"
    echo "	-d: directory where the project is created (by default the project is generated in the current dir)"
    echo "	-t: type of the project to generate (can be \"ds\", \"dm\", \"service\" or \"dm.activator\"). Default=\"ds\""
    exit 1
}

#
# Create eclipse .project and .metadata files
#
function create_eclipse_files() {
    cat <<EOF > $DIR/$PROJECT/.project
<?xml version="1.0" encoding="UTF-8"?>
<projectDescription>
	<name>$PROJECT</name>
	<comment></comment>
	<projects>
	</projects>
	<buildSpec>
		<buildCommand>
			<name>org.eclipse.jdt.core.javabuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
		<buildCommand>
			<name>bndtools.core.bndbuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
	</buildSpec>
	<natures>
		<nature>org.eclipse.jdt.core.javanature</nature>
		<nature>bndtools.core.bndnature</nature>
	</natures>
</projectDescription>
EOF

    cat <<'EOF' > $DIR/$PROJECT/.classpath
<?xml version="1.0" encoding="UTF-8"?>
<classpath>
	<classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-1.8"/>
	<classpathentry kind="con" path="aQute.bnd.classpath.container"/>
	<classpathentry kind="src" output="bin" path="src"/>
	<classpathentry kind="src" output="bin_test" path="test">
		<attributes>
			<attribute name="test" value="true"/>
		</attributes>
	</classpathentry>
	<classpathentry kind="output" path="bin"/>
</classpath>
EOF
}

#
# Create a declarative service project
#
function create_project_service() {
    echo "Creating service project $PROJECT in $DIR"

    mkdir -p $DIR/$PROJECT
    SRCDIR=`echo $PROJECT | sed "s|\.|/|g"`
    mkdir -p $DIR/$PROJECT/src/$SRCDIR
    cat <<EOF > $DIR/$PROJECT/src/$SRCDIR/Service.java
package $PROJECT;

import org.osgi.annotation.versioning.*;

@ProviderType
public interface Service {

}
EOF

    echo "version 1.0.0" >  $DIR/$PROJECT/src/$SRCDIR/packageinfo
    cat <<EOF > $DIR/$PROJECT/bnd.bnd
-buildpath: \\
	osgi.annotation;version='7.0.0',\\
	osgi.core;version='7.0',\\
	osgi.cmpn;version='7.0'


# for multi-jar projects, uncomment the following, and move other specific declaration to other sub bundles
#-sub: *.bnd

-testpath: \
	${junit}

Bundle-Version: 1.0.0
Export-Package: $PROJECT
EOF

    mkdir -p $DIR/$PROJECT/test
    echo > $DIR/$PROJECT/test/.keepme

    create_eclipse_files
}

#
# Create a declarative service project
#
function create_project_ds() {
    echo "Creating declarative service project $PROJECT in $DIR"

    SRCDIR=`echo $PROJECT | sed "s|\.|/|g"`
    mkdir -p $DIR/$PROJECT/src/$SRCDIR
    
    cat <<EOF > $DIR/$PROJECT/src/$SRCDIR/ComponentImpl.java
package $PROJECT;

import org.osgi.service.component.annotations.*;
import org.apache.log4j.*;

@Component
public class ComponentImpl {
    private final static Logger _log = Logger.getLogger(ComponentImpl.class);

    @Activate
    void start() {
        _log.warn("Starting");
    }
}
EOF

    cat <<EOF > $DIR/$PROJECT/bnd.bnd
-buildpath: \\
	osgi.annotation;version='7.0.0',\\
	osgi.core;version='7.0',\\
	osgi.cmpn;version='7.0',\\
	org.darkphoenixs:log4j;version=1.2.17

# for multi-jar projects, uncomment the following, and move other specific declaration to other sub bundles
#-sub: *.bnd

-testpath: \\
	\${junit}

Bundle-Version: 1.0.0
Private-Package: $PROJECT
EOF

    mkdir -p  $DIR/$PROJECT/test/$SRCDIR
    cat <<EOF > $DIR/$PROJECT/test/$SRCDIR/ExampleTest.java
package $PROJECT;

import static org.junit.Assert.*;
import org.junit.Test;

public class ExampleTest {
	@Test
	public void test() {
		fail("Not yet implemented");
	}
}
EOF
    
    create_eclipse_files
}

#
# Create a declarative service project
#
function create_project_dm() {
    echo "Creating dependency manager project $PROJECT in $DIR"

    SRCDIR=`echo $PROJECT | sed "s|\.|/|g"`
    mkdir -p $DIR/$PROJECT/src/$SRCDIR
    cat <<EOF > $DIR/$PROJECT/src/$SRCDIR/ComponentImpl.java
package $PROJECT;

import org.apache.felix.dm.annotation.api.*;
import org.apache.log4j.*;

@Component
public class ComponentImpl {
    private final static Logger _log = Logger.getLogger(ComponentImpl.class);

    @Start
    void start() {
        _log.warn("Starting");
    }
}
EOF
    
    cat <<EOF > $DIR/$PROJECT/bnd.bnd
-buildpath: \\
	osgi.annotation;version='7.0.0',\\
	osgi.core;version='7.0',\\
	osgi.cmpn;version='7.0',\\
	org.darkphoenixs:log4j;version=1.2.17,\\
	org.apache.felix.dependencymanager.annotation;version=4.2

# for multi-jar projects, uncomment the following, and move other specific declaration to other sub bundles
#-sub: *.bnd

-testpath: \\
	\${junit}

Bundle-Version: 1.0.0
Private-Package: $PROJECT
EOF

    mkdir -p  $DIR/$PROJECT/test/$SRCDIR
    cat <<EOF > $DIR/$PROJECT/test/$SRCDIR/ExampleTest.java
package $PROJECT;

import static org.junit.Assert.*;
import org.junit.Test;

public class ExampleTest {
	@Test
	public void test() {
		fail("Not yet implemented");
	}
}
EOF

    create_eclipse_files    
}

#
# Create a dependency manager activator project
#
function create_project_dm_activator() {
    echo "Creating dependency manager api project $PROJECT in $DIR"

    SRCDIR=`echo $PROJECT | sed "s|\.|/|g"`
    mkdir -p $DIR/$PROJECT/src/$SRCDIR
    
    cat <<EOF > $DIR/$PROJECT/src/$SRCDIR/Activator.java
package $PROJECT;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.Component;
import org.apache.felix.dm.ServiceDependency;
import org.osgi.framework.*;
import org.apache.log4j.*;

public class Activator extends DependencyActivatorBase {
    @Override
    public void init(BundleContext ctx, DependencyManager dm) throws Exception {
            Component comp = createComponent()
                .setImplementation(ComponentImpl.class);
            dm.add(comp);
    }
}
EOF

    cat <<EOF > $DIR/$PROJECT/src/$SRCDIR/ComponentImpl.java
package $PROJECT;

import org.apache.log4j.*;

public class ComponentImpl {
    private final static Logger _log = Logger.getLogger(ComponentImpl.class);

    void start() {
        _log.warn("Starting");
    }
}
EOF
    
    cat <<EOF > $DIR/$PROJECT/bnd.bnd
-buildpath: \\
	osgi.annotation;version='7.0.0',\\
	osgi.core;version='7.0',\\
	osgi.cmpn;version='7.0',\\
	org.darkphoenixs:log4j;version=1.2.17,\\
	org.apache.felix.dependencymanager;version=4.4

# for multi-jar projects, uncomment the following, and move other specific declaration to other sub bundles
#-sub: *.bnd

-testpath: \\
	\${junit}

Bundle-Version: 1.0.0
Bundle-Activator: ${PROJECT}.Activator
Private-Package: $PROJECT
EOF

    mkdir -p  $DIR/$PROJECT/test/$SRCDIR
    cat <<EOF > $DIR/$PROJECT/test/$SRCDIR/ExampleTest.java
package $PROJECT;

import static org.junit.Assert.*;
import org.junit.Test;

public class ExampleTest {
	@Test
	public void test() {
		fail("Not yet implemented");
	}
}
EOF

    create_eclipse_files    
}

# Check parameters
while  [ ! $# = 0 ]
do case $1 in
       -h|-help|--help)
	   usage
	   ;;
       -d|--dir)
	   shift
	   DIR=$1
	   ;;
       -p|--project)
	   shift
	   PROJECT=$1
	   ;;
       -t)
	   shift
	   TYPE=$1
	   ;;
   esac
   shift
done

[ "$PROJECT" == "" ] && echo "*** Missing -p option" && usage && exit 1
[ "$TYPE" == "" ] && echo "*** Missing -t option" && usage && exit 1
[ ! -d "$DIR" ] && echo "*** Directory $DIR does not exist" && usage && exit 1
[ -d "$DIR/$PROJECT" ] && echo "*** Directory $PROJECT already exist" && usage && exit 1

case $TYPE in
    ds)
	create_project_ds
	;;
    dm)
	create_project_dm
	;;
    dm.activator)
	create_project_dm_activator
	;;
    service)
	create_project_service
	;;

    *)
	echo "Wrong project type: $TYPE"
	usage
	;;
esac	




