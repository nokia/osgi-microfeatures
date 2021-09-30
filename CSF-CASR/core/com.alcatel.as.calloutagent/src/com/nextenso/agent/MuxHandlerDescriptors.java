package com.nextenso.agent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Dictionary;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.alcatel.as.service.metering2.Meter;
import com.alcatel.as.service.metering2.StopWatch;

import alcatel.tess.hometop.gateways.utils.Config;
import alcatel.tess.hometop.gateways.utils.Log;

/**
 * This class collects all mux handler descriptors found either from $INSTALL_DIR/resouce/calloutAgent/*.desc, or from 
 * any bundle that have a "ASR-MuxHandlerDesc" header.
 */
public class MuxHandlerDescriptors {
    public final static String DESC_MANIFEST = "ASR-MuxHandlerDesc";

    private final static Log _log = Log.getLogger("callout.desc");
    private final static String LEGACY_DESCRIPTOR_DIR = "calloutAgent";
    private final static String SUFFIX_DESC = ".desc";
    private final Map<String, MuxHandlerDesc> _descriptors = new ConcurrentHashMap<String, MuxHandlerDesc>();
    
    /**
     * Component is starting. Collect all descriptors from resouce/calloutAgent/*.desc, 
     * and from all bundles manifest entries.
     */
    void scanBundles(BundleContext bctx) {
        URL resourceDir = ClassLoader.getSystemResource(LEGACY_DESCRIPTOR_DIR);
        if (resourceDir != null) {
            File[] files = new File(resourceDir.getPath()).listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(SUFFIX_DESC);
                }
            });

            if (files != null) {
                for (File file : files) {
                    try {
                        URL desc = file.toURI().toURL();
                        URL flags = null;
                        File flagsFile = new File(getFlagsPath(file.getPath()));
                        if (flagsFile.exists()) {
                            flags = flagsFile.toURI().toURL();
                        }

                        MuxHandlerDesc mhd = getFromURL(desc, flags);
                        _log.debug("Loaded legacy mux handler desc from " + file + ":" + mhd);
                        _descriptors.put(mhd.getProtocol().toLowerCase(), mhd);
                    } catch (IOException e) {
                        _log.warn("Could not load legacy mux handler descriptor " + file, e);
                        continue;
                    }
                }
            }
        }
        
        // Scan all bundles, if we have a non null bundle context.
        if (bctx != null) {
            for (Bundle b : bctx.getBundles()) {
                loadDescriptors(b);
            }
        }

        _log.debug("Found mux handlers descriptors: %s", _descriptors);
    }

    public MuxHandlerDesc getFromServiceProperties(String protocol, Dictionary<String, Object> muxHandlerServiceProperties) {
    	// Build MuxHandlerDesc from service properties. So far, some part of mux handler desc (hidden, appName, appId, flags, elasticity) 
    	// was loaded statically from .desc files found from classpath or from bundles META-INF/*.desc files, and
    	// other part (autoreporting, protocol) was loaded from service properties.    	
    	// Now we assume that mux handler can provide all mux handler desc infos from service properties.
    	MuxHandlerDesc staticDesc = _descriptors.get(protocol.toLowerCase()); // possibly null
    	return new MuxHandlerDesc(muxHandlerServiceProperties, staticDesc);    	
    }

    /**
     * Returns a Mux Handler descriptor matching a given protocol, or null.
     */
    public MuxHandlerDesc getFromProtocol(String protocol) {
        return _descriptors.get(protocol.toLowerCase());
    }
    
    public Map<String, MuxHandlerDesc> getMuxHandlerDescriptors() {
        return _descriptors;
    }
    
    public static MuxHandlerDesc getFromResource(String desc) throws IOException {
        Properties descProp = new Properties();
        Properties flagsProp = null;
        load(descProp, getResourceAsStream(desc));
        String flags = getFlagsPath(desc);
        InputStream flagsIn = getResourceAsStream(flags);
        if (flagsIn != null) {
          flagsProp = new Properties();
          load(flagsProp, flagsIn);
        }
        return new MuxHandlerDesc(descProp, null, flagsProp);
    }
    
    private MuxHandlerDesc getFromURL(URL desc, URL flags) throws IOException {
        // Load the mux handler desc and the optional mux handler flags
        Properties descProp = new Properties();
        Properties flagsProp = null;
        load(descProp, desc.openStream());
        if (flags != null) {
            flagsProp = new Properties();
            load(flagsProp, flags.openStream());
        }
        MuxHandlerDesc mhd = new MuxHandlerDesc(descProp, null, flagsProp);
        return mhd;
    }

    private static String getFlagsPath(String path) {
        int lastDot = path.lastIndexOf(".");
        if (lastDot != -1) {
            return new StringBuilder(path.substring(0, lastDot)).append(".flags").toString();
        } else {
            return new StringBuilder(path).append(".flags").toString();
        }
    }

    private static void load(Properties props, InputStream in) throws IOException {
        try {
            props.load(in);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
            }
        }
    }
    
    /**
     * A bundle having a "ASR-MuxHandlerDesc" header is found: register its internal mux handler descriptor.
     */
    private void loadDescriptors(Bundle bundle) {
        String descLocation = (String) bundle.getHeaders().get(DESC_MANIFEST);
        if (descLocation != null) {
            try {
                URL desc = bundle.getResource(descLocation);
                if (desc == null) {
                    _log.warn("Did not find mux handler desc in bundle %s (invalid ASR-MuxHandlerDesc header: %s)",
                        bundle.getLocation(), descLocation);
                }

                URL flags = bundle.getResource(getFlagsPath(descLocation));
                MuxHandlerDesc muxHandlerDesc = getFromURL(desc, flags);
                _log.debug("Loaded mux handler desc from bundle: location=%s, desc=%s", bundle.getLocation(), muxHandlerDesc);
                _descriptors.put(muxHandlerDesc.getProtocol().toLowerCase(), muxHandlerDesc);
            } catch (IOException e) {
                _log.warn("invalid ASR-MuxHandlerDesc header (%s) from bundle %s", descLocation, bundle.getLocation());
            }
        }
    }
    
    private static InputStream getResourceAsStream(String resource) throws IOException {
        String file = resource;
        
        if (resource.charAt(0) != '/') {
          resource = '/' + resource;
        }
        
        InputStream in = Config.class.getResourceAsStream(resource);
        
        if (in == null) {
          in = Object.class.getResourceAsStream(resource);
        }
        
        if (in == null) {
          ClassLoader ccl = Thread.currentThread().getContextClassLoader();
          if (ccl != null) {
            in = ccl.getResourceAsStream(resource);
          }
        }
        
        if (in == null) {
          File f = new File(file);
          if (f.exists()) {
            in = new FileInputStream(file);
          }
        }
        
        if (in == null) {
          if (file.charAt(0) == '/') {
            file = file.substring(1);
          }
          in = ClassLoader.getSystemClassLoader().getResourceAsStream(file);
        }
        
        return in;
      }
}
