package com.nokia.as.autoconfig.sys;

import static com.nokia.as.autoconfig.Utils.newMap;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.nokia.as.autoconfig.Configuration;
import com.nokia.as.autoconfig.Utils;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Utils.class })
public class SystemPropertyResolverTest {
    
    private SystemPropertyResolver resolver = new SystemPropertyResolver();
    
    @Before
    public void setUp() {
        PowerMockito.mockStatic(Utils.class);
        when(Utils.newMap(Matchers.<String>anyVararg())).thenCallRealMethod();
        Map<String, String> env = new HashMap<>();
        env.put("envProp", "envValue");
        env.put("aPid:aKey", "envValue");
        env.put("aPid:aKey2", "envValue");
        env.put("bPid:bKey", "bValue");
        Properties sys = new Properties();
        sys.put("sysProp", "sysValue");
        sys.put("aPid:aKey", "sysValue");
        sys.put("aPid:aKey3", "sysValue");
        sys.put("cPid:cKey", "cValue");
        when(Utils.getEnvProperties()).thenReturn(env);
        when(Utils.getSystemProperties()).thenReturn(sys);
    }
    
    @Test
    public void systemPropertyResolverTest() {
        resolver.resolve();
        Configuration expected = new Configuration();
        expected.config.put("aPid", newMap("aKey", "sysValue", "aKey2", "envValue", "aKey3", "sysValue", Configuration.AUTOCONF_ID, "true"));
        expected.config.put("bPid", newMap("bKey", "bValue", Configuration.AUTOCONF_ID, "true"));
        expected.config.put("cPid", newMap("cKey", "cValue", Configuration.AUTOCONF_ID, "true"));
        
        assertEquals(expected, resolver.config());
    }

}
