package com.nokia.as.autoconfig;

import static org.osgi.framework.Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import org.junit.After;
import org.junit.Before;

import com.nokia.as.osgi.launcher.OsgiLauncher;

public class ResolverTestBase {
    
    protected OsgiLauncher framework;

    protected final String CURRENT_DIR = System.getProperty("user.dir") + File.separator;

    //Bundle resolver tests
    protected final String GENERATED = CURRENT_DIR + "generated" + File.separator;
    protected final String DEF_JAR = GENERATED + "com.nokia.as.autoconfig.test.def.jar";
    protected final String FACTORY_JAR = GENERATED + "com.nokia.as.autoconfig.test.factory.jar";
    protected final String BND1_JSON_JAR = GENERATED + "com.nokia.as.autoconfig.test.bnd1.json.jar";
    protected final String BND2_JSON_JAR = GENERATED + "com.nokia.as.autoconfig.test.bnd2.json.jar";
    protected final String BND_FACTORY_JSON_JAR = GENERATED + "com.nokia.as.autoconfig.test.bnd.factory.json.jar";
    protected final String BND1_YAML_JAR = GENERATED + "com.nokia.as.autoconfig.test.bnd1.yaml.jar";
    protected final String BND2_YAML_JAR = GENERATED + "com.nokia.as.autoconfig.test.bnd2.yaml.jar";
    protected final String BND_FACTORY_YAML_JAR = GENERATED + "com.nokia.as.autoconfig.test.bnd.factory.yaml.jar";

    //File resolver tests
    protected final String TEST_DIR = "test-confDirs" + File.separator;
    protected final String SIMPLE_CONFDIR_CFG = TEST_DIR + "conf-test-cfg";
    protected final String SIMPLE_CONFDIR_CFG_TRIM = TEST_DIR + "conf-test-cfg-trim";
    protected final String SIMPLE_CONFDIR_CFG_PATCH = TEST_DIR + "conf-test-cfg-patch";
    protected final String SIMPLE_CONFDIR_EXTRA = TEST_DIR + "conf-test-extra";
    protected final String SIMPLE_CONFDIR_EXTRA_PATCH = TEST_DIR + "conf-test-extra-patch";
    protected final String SIMPLE_CONFDIR_YAML = TEST_DIR + "conf-test-yaml";
    protected final String FACTORY_CONFDIR_CFG = TEST_DIR + "conf-testFactory-cfg";
    protected final String FACTORY_CONFDIR_EXTRA = TEST_DIR + "conf-testFactory-extra";
    protected final String FACTORY_CONFDIR_YAML = TEST_DIR + "conf-testFactory-yaml";
    protected final String FACTORY_CONFDIR_PATCH = TEST_DIR + "conf-testFactory-patch";
    protected final String FILE_CONFDIR = TEST_DIR + "conf-testFile";
    protected final String FILE_CONFDIR_ENV = TEST_DIR + "conf-testFile-env";
    protected final String FILE_CONFDIR_EXTRA_MISSING = TEST_DIR + "conf-testFile-extra-missing";
    protected final String FILE_CONFDIR_EXTRA_OVERRIDE = TEST_DIR + "conf-testFile-extra-override";

    @Before
    public void setUp() throws Exception {
        ServiceLoader <OsgiLauncher> servLoad = ServiceLoader.load(OsgiLauncher.class);
        Map<String, String> config = new HashMap<>();
        config.put(FRAMEWORK_SYSTEMPACKAGES_EXTRA, "org.apache.log4j; version=1.2.17," +
                                                   "org.apache.log4j.spi; version=1.2.17," +
                                                   "org.osgi.service.event; version=1.4.0," +
                                                   "com.nokia.as.autoconfig.test.bundle.api; version=1.0.0");
        framework = servLoad.iterator().next();
        framework = framework.withFrameworkConfig(config)
                             .withBundles(Utils.url(GENERATED + "com.nokia.as.autoconfig.jar").toString(),
                                          Utils.url(GENERATED + "com.nokia.as.autoconfig.test.api.jar").toString())
                             .useDirectory(CURRENT_DIR + "/dependencies");
    }
    
    @After
    public void tearDown() {
        System.clearProperty(Activator.CONFIG_DIR);
        framework.stop(0);
        framework = null;
    }

}
