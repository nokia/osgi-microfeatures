// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.bundleinstaller.impl;

import static java.lang.System.err;
import static java.lang.System.out;
import static org.osgi.framework.Constants.BUNDLE_ACTIVATOR;
import static org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.service.startlevel.StartLevel;

import com.alcatel.as.service.bundleinstaller.BundleInstaller;

/**
 * Bundle Installer, which installs bundles from some given directories.
 */
public class BundleInstallerImpl implements BundleActivator, BundleInstaller, FrameworkListener {
    public final static String BUNDLE_INSTALLER_CONTEXT = "com.alcatel.as.service.bundleinstaller.bundlecontext";
    /**
     *  Bundle Symbolic Name -> DeployedBundle
     */
    private HashMap<File, DeployedBundle> _deployedBundles = new LinkedHashMap<File, DeployedBundle>();

    /**
     * The bundle installer has this service property set to "true", when all bundles have been deployed and started.
     */
    public final static String DEPLOYED = "deployed";

    private volatile Thread _runner = null;
    private volatile boolean _runing = false;
    private BundleContext _bc;
    private File[] _deployDirs;
    private Filter _whiteList;
    private Filter _blackList;
    private File _firstDir; // this directory takes precedence over other directories. (aka "custo")
    private long _interval = Long.MAX_VALUE;
    private boolean _autoStartApps;
    private StartLevel _startLevel;

    private final static String REGISTER_BC_IN_SYSTEM_PROPS = "bundleinstaller.registerBundleContext";

    private final static String FELIX_SMB = "org.apache.felix.main";

    private static boolean DEBUG;
    private final static String PROP_AUTOSTART_APPS = "bundleinstaller.autoStartApps";
    private final static String PROP_DEPLOY_DIRS = "bundleinstaller.deploydirs";
    private final static String PROP_INTERVAL = "bundleinstaller.poll";
    private final static String PROP_WHITE_LIST = "bundleinstaller.whitelist";
    private final static String PROP_BLACK_LIST = "bundleinstaller.blacklist";
    private final static String DEFAULT_DEPLOY_DIRS = "bundles/custo:bundles/repository/core:bundles/repository/apps";
    private final static String DEPLOYED_AT_STARTUP = "bundleinstaller.deployedAtStartup";

    // Set BundleInstaller "deployed" service property once all bundle have been started, else
    // do this when the startDeployedApplications method is called.
    private Boolean _deployedAtStartup;

    /**
     * Are we refreshing bundles ?
     */
    private volatile boolean _refreshing;
    
    /**
     * Name of an optional system property file which refers to a txt file which contains specific bundle start levels.
     * The file must contains the following syntax:
     * 
     * startlevel.N=<list of bundle symbolic name sperated by spaces, or comma>
     */
    private final static String START_LEVEL_FILE = "bundleinstaller.startlevel";
    
    /**
     * default start level property file used in case the START_LEVEL_FILE system property file is not available
     */
    private final static String DEFAULT_START_LEVEL = "META-INF/startlevel.txt";
    
    /**
     * Helper class used to check bundle start levels
     */
    private StartLevels _startLevels;
    
    /**
     * Bundle installer log levels
     */
    private enum Level {
    	ERROR,
    	WARN,
    	INFO,
    	DEBUG
    };
    
    private Level _logLevel;
    
    public void start(final BundleContext bc) {
        try {
            _bc = bc;
            _logLevel = parseLogLevel();
            
            DEBUG = Boolean.valueOf(getProperty("bundleinstaller.debug", "false"));
            
            loadStartLevels();

            // Store our bundle context in a well known properties so any other
            // component can get it.
            boolean registerBundleContext = true;
            if ("false".equals(bc.getProperty(REGISTER_BC_IN_SYSTEM_PROPS))) {
                registerBundleContext = false;
            }
            if (registerBundleContext) {
                System.getProperties().put(BUNDLE_INSTALLER_CONTEXT, _bc);
            }

            // Load start level service
            ServiceReference ref = bc.getServiceReference(StartLevel.class.getName());
            if (ref == null) {
                throw new RuntimeException("BundleInstaller: Could not find start level service");
            }
            _startLevel = (StartLevel) bc.getService(ref);

            // Init dirs to be scanned
            _deployDirs = getConfDirs(PROP_DEPLOY_DIRS, DEFAULT_DEPLOY_DIRS);
            _firstDir = _deployDirs[0];

            // Load parameters used to start bundles.
            _whiteList = getFilter(PROP_WHITE_LIST);
            _blackList = getFilter(PROP_BLACK_LIST);
            _autoStartApps = Boolean.valueOf(getProperty(PROP_AUTOSTART_APPS, "false"));
            log(Level.DEBUG, "blacklist=%s", _blackList);
            log(Level.DEBUG, "whitelist=%s", _whiteList);
            log(Level.DEBUG, "autoStart=%s", _autoStartApps);

            // Init sleep interval for scanner thread.
            String pollInterval = getProperty(PROP_INTERVAL, null);
            if (pollInterval != null) {
                _interval = Long.valueOf(pollInterval);
            }

            _deployedAtStartup = Boolean.valueOf(getProperty(DEPLOYED_AT_STARTUP, "true"));

            log(Level.DEBUG, "%s starting: deployedAtStartup=%s, deploy dirs=%s", 
            		bc.getBundle().getLocation(),
            		_deployedAtStartup,
            		java.util.Arrays.toString(_deployDirs));
            if (_interval != Long.MAX_VALUE) {
            	log(Level.DEBUG, "bundle installer hot deployer active: polling interval=%s ms.", _interval);
            }
            		
            // We'll start installing/updating/uninstalling bundles only once
            // the fwk has finished its initialization.
            // Indeed, if we install/update/uninstall bundles in our start
            // method, then we may mess around with the fwk initialization processing ...

            if (_bc.getBundle(0).getState() != Bundle.ACTIVE) {
                _bc.addFrameworkListener(this);
            } else {
                frameworkEvent(new FrameworkEvent(FrameworkEvent.STARTED, _bc.getBundle(0)));
            }
        }

        catch (Throwable t) {
        	log(Level.ERROR, t, "Error while starting bundle installer");
        }
    }

	private void loadStartLevels() throws FileNotFoundException, IOException {
		boolean loadDefaultStartLevels = true;
		
        _startLevels = new StartLevels();
        String startLevelFile = _bc.getProperty(START_LEVEL_FILE);
        if (startLevelFile != null) {
        	File f = new File(startLevelFile);
        	if (f.exists()) {
        		_startLevels.load(new FileInputStream(f));
        		loadDefaultStartLevels = false;
        	}
        } 
        if (loadDefaultStartLevels) {
        	_startLevels.load(getClass().getClassLoader().getResourceAsStream(DEFAULT_START_LEVEL));    
        }
	}

	/**
     * The framework has started: we can now start
     * installing/updating/uninstalling bundles ...
     */
    public void frameworkEvent(FrameworkEvent event) {
        try {
            switch (event.getType()) {
            case FrameworkEvent.STARTED:
                // Load already installed bundles and stop them.
                loadAlreadyInstalledBundles();

                // Scan for new or for already installed bundles to be updated
                boolean lost = scanForLostBundles();
                for (int i = 0; i < _deployDirs.length; i++) {
                    scanForNewBundles(_deployDirs[i]);
                }
                if (lost || scanForUpdatedBundles()) {
                    // some bundles were updated. needs to refresh.
                    Refresher refresher = new Refresher(this::finishInitialisation);
                    refresher.refresh(); // asynchronous.
                } else {
                    finishInitialisation();
                }
                break;

            case FrameworkEvent.ERROR:
            	log(Level.ERROR, event.getThrowable(), "Got Framework error event: bundle=%s", event.getBundle().getSymbolicName());
                break;
            }
        }

        catch (Throwable t) {
        	log(Level.ERROR, t, "Error while handling framework event");
        }
    }

    protected void finishInitialisation() {
        startBundles();

        // We are now ready: register our service. If someone invoke our "deployApplications"
        // method, then we'll handle the method in the scanner thread.
        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put(DEPLOYED, Boolean.valueOf(_deployedAtStartup).toString());
		log(Level.DEBUG, "Registering BundleInstaller service with service property \"%s\"=%s", 
			DEPLOYED,
			_deployedAtStartup);
        _bc.registerService(BundleInstaller.class.getName(), BundleInstallerImpl.this, properties);

        if (_interval != Long.MAX_VALUE) {
            // Start scanner thread only if hotdeploy
            // feature is active.
            startHotDeployerThread();
        }
    }

    private File getFile(Bundle b) {
        try {
            String location = b.getLocation();
            if (location.startsWith("file:")) {
                return new File(location.substring("file:".length()));
            } else if (location.startsWith("reference:file:")) {
                return new File(location.substring("reference:file:".length()));
            }
            return new File(new URL(location).getFile());
        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not get location from bundle " + b.getLocation(), e);
        }
    }

    public void stop(BundleContext bc) {
        stop();
        _bc = null;
    }

    // ----------------------------- BundleInstaller

    public Map<String, Bundle> deployApplications(final List<String> appNames) throws Exception {
        return Collections.emptyMap();
    }

    public void startDeployedApplications() {
    }

    public Collection<Bundle> getDeployedApplicationBundles() {
        return Collections.emptyList();
    }

    public void shutdown() {
        log(Level.DEBUG, "shutdown requested...");
        // First, stop the declarative service
        for (Bundle b : _bc.getBundles()) {
            if (b.getSymbolicName().equals("org.apache.felix.scr")) {
                try {
                	log(Level.DEBUG, "Stopping %s", getSimpleName(b));
                    b.stop(Bundle.STOP_TRANSIENT);
                } catch (Throwable t) {
                	log(Level.WARN, t, "Could not stop DeclarativeService");
                }
                break;
            }
        }

        try {
            log(Level.INFO, "Stopping OSGi Framework ... ");
            _bc.addFrameworkListener(new FrameworkListener() {
                public void frameworkEvent(FrameworkEvent arg0) {
                    log(Level.INFO, "Framework stopped. exiting ...");
                    System.exit(1);
                }
            });
            _bc.getBundle(0).stop();
            Thread.sleep(30000); // we give 30 seconds to let the fwk stop gracefully;
        }

        catch (Throwable t) {
            log(Level.ERROR, t, "Could not stop OSGi framework");
        }

        log(Level.DEBUG, "Framework did not stop timely, exiting ...");
        System.exit(0);
    }

    // ----------------------------- Private methods ---------------------------------------

    private String getBundleKey(Bundle b) {
        return getFile(b).getPath().replace(File.separator, "_");
    }

    /**
     * Load already installed bundles.
     */
    private void loadAlreadyInstalledBundles() {
        for (Bundle b : _bc.getBundles()) {
            if (b.getBundleId() == 0 || b.getBundleId() == _bc.getBundle().getBundleId()
                || "BundleInstaller".equals(b.getSymbolicName()) || b.getLocation().startsWith("obr"))
            {
                log(Level.INFO, "ignoring %s", b.getLocation());
                continue; // ignore ourself, as well as fwk system bundle
            }

            if (!b.getLocation().startsWith("file") && !b.getLocation().startsWith("reference:file:")) {
                log(Level.INFO, "Ignoring already installed bundle %s", b.getLocation());
                continue; // only accept file bundles.
            }

            try {
                File loc = getFile(b);
                if (pathStartsWith(loc, _deployDirs)) {
                    File f = loc;
                    // if the bundle's location does not exists, or if the current bundle start level does
                    // not match our current fwk start level, then uninstall the bundle from the cache.
                    // we'll reinstall it later.
                    if (!f.exists() || _startLevel.getBundleStartLevel(b) != _startLevel.getStartLevel()) {
                        uninstall(b);
                        continue;
                    }
                    
                    // there is a bug in Felix: bundles with native code are using felix cache even when using bundle URL "references".
                    // so we need to uninstall previously installed bundles with native code, in order to ensure we load 
                    // the latest version (not previous vesion from cache).
                    if (b.getHeaders().get(Constants.BUNDLE_NATIVECODE) != null) {
                        uninstall(b);
                        continue;
                    }

                    DeployedBundle dp = new DeployedBundle(b);
                    _deployedBundles.put(f, dp);
                    log(Level.INFO, "Cached bundle: %s", getSimpleName(b));
                }
            }

            catch (Exception e) {
                log(Level.ERROR, e, "Problem while scanning already installed bundle %s", b.getLocation());
                uninstall(b);
            }
        }

        log(Level.DEBUG, "found already installed bundles: %s", _deployedBundles);
    }

    private void uninstallAll() {
        Bundle myBundle = _bc.getBundle();
        for (Bundle b : _bc.getBundles()) {
            if (b.getBundleId() == 0 || b.getBundleId() == myBundle.getBundleId()
                || b.getLocation().startsWith("obr"))
            {
                continue;
            }
            uninstall(b, false);
        }
    }

    private void uninstall(Bundle b) {
        uninstall(b, true);
    }

    private void uninstall(Bundle b, boolean log) {
    	log(Level.INFO, "Uninstalling " + getSimpleName(b));
        try {
            b.stop(Bundle.STOP_TRANSIENT);
        } catch (Throwable t) {
        }
        try {
            b.uninstall();
        } catch (Throwable t) {
            log(Level.WARN, t, "Could not uninstall %s", getSimpleName(b));
        }
    }
    
    private WatchService createDeployDirsWatcher() throws IOException {
    	WatchService watcher = FileSystems.getDefault().newWatchService();
		Path[] paths = Stream.of(_deployDirs).map(file -> file.toPath()).toArray(Path[]::new);
		for (Path path : paths) {
			if (path.toFile().exists()) {
				path.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
						
			}
		}
		return watcher;
    }

    private void startHotDeployerThread() {
        if (_runner != null) {
            return;
        }

        _runner = new Thread("BundleInstaller") {
            public void run() {
            	WatchService watcher = null;
				try {
					watcher = createDeployDirsWatcher();
            	} catch (Exception e) {
            		log(Level.ERROR, e, "Could not initialize bundle directory watcher");
            		return;
            	}

                while (_runing) {
                    try {
                    	// poll for deployment directory changes.
        				WatchKey watchKey = watcher.take();
        				
                    	// check if some bundles must be updated
						try {
							List<WatchEvent<?>> events = watchKey.pollEvents();
							if (events.size() > 0) {
		        				// some bundles were updated, wait a bit before updating (in case some bundles are still being copied)
								Thread.sleep(_interval);
								
								// some bundles have been updated (or added/removed); rescan
								doScan();
							}
						} finally {
							watchKey.reset();
						}
                    }

                    catch (Throwable t) {
                        if (_runing) {
                        	log(Level.ERROR, t, "scan failed");
                        }
                    }
                }
                log(Level.INFO, "stopped scan");
                if (watcher != null) {
					try {
						watcher.close();
					} catch (IOException e1) {
					}
                }
                stopAll();
            }
        };
        _runing = true;
        _runner.start();
    }

    /**
     * Stop the scanner thread if not already stopped. Also unregister the config object.
     */
    void stop() {
    	log(Level.DEBUG, "BundleDeployer stopping.");

        if (_runner == null) {
            return;
        }

        try {
            _runing = false;
            _runner.interrupt();
            _runner.join();
            log(Level.DEBUG, "BundleDeployer stopped.");
        } catch (Exception ignored) {
        }
        _runner = null;
    }

    /**
     * Scan for new, updated and lost files.
     */
    void doScan() {
        if (_refreshing) {
            return;
        }
        log(Level.INFO, "Scanning updated bundles ...");
        boolean uninstalled = scanForLostBundles();
        for (int i = 0; i < _deployDirs.length; i++) {
            scanForNewBundles(_deployDirs[i]);
        }
        boolean updated = scanForUpdatedBundles();
        if (uninstalled || updated) {
            _refreshing = true;
            refresh(() -> {
				try {
					startBundles();
				} finally {
					_refreshing = false;
				}
            });
        } else {
            startBundles();
        }
    }

    boolean scanForLostBundles() {
        boolean uninstalled = false;
        // check, uninstall and copy to removed vector as necessary
        for (Iterator<DeployedBundle> it = _deployedBundles.values().iterator(); it.hasNext();) {
            DeployedBundle dp = it.next();
            File f = dp.getFile();

            // If file does not exist anymore OR if file has not been loaded from _firstDir AND the
            // bundle is present in _firstDir, then consider the file as lost, in order to load the
            // one from the _firstDir, which takes precendence.

            if (!f.exists() || (!pathStartWith(f, _firstDir) && isFileInDir(f, _firstDir))) {
                dp.stop();
                dp.uninstall();
                it.remove();
                uninstalled = true;
            }
        }
        return uninstalled;
    }

    void scanForNewBundles(File dir) {
        if (dir != null && dir.exists() && dir.isDirectory()) {
            String[] files = listBundlesFromDir(dir);
            for (int i = 0; files != null && i < files.length; i++) {
                try {
                    File f = new File(dir + File.separator + files[i]);

                    // if the file is also in custo, ignore.
                    if (!BundleInstallerImpl.this.pathStartWith(f, _firstDir)
                        && BundleInstallerImpl.this.isFileInDir(f, _firstDir))
                    {
                    	log(Level.DEBUG, "Ignoring %s (already in custo)", f);
                        continue;
                    }

                    DeployedBundle dp = _deployedBundles.get(f);
                    if (dp == null) {
                        dp = new DeployedBundle(f);
                        // Install if not currently installed, or store the bundle in delayed startlist.
                        if (dp.install()) {
                            _deployedBundles.put(f, dp);
                        }
                    }
                }

                catch (Throwable t) {
                	log(Level.WARN, t, "scan failed");
                }
            }
        }
    }

    boolean scanForUpdatedBundles() {
        boolean updated = false;
        for (Iterator<DeployedBundle> it = _deployedBundles.values().iterator(); it.hasNext();) {
            DeployedBundle dp = it.next();
            if (dp.needsUpdate()) {
                if (dp.update()) {
                    updated = true;
                }
            }
        }
        return updated;
    }

    void refresh(Runnable callback) {
        Refresher refresher = new Refresher(callback);
        refresher.refresh();
    }

    void startBundles() {
        if (_interval == Long.MAX_VALUE /* not hot deploy */) {
            log(Level.DEBUG, "Starting bundles ...");
        }

        for (DeployedBundle db : getDeployedBundles()) {
            try {
                db.start();
            } catch (Exception ex) {
                log(Level.WARN, ex, "Could not start bundle: %s", db._location);
            }
        }
    }

    void stopAll() {
        for (DeployedBundle db : getDeployedBundles()) {
            db.stop();
        }
        _deployedBundles.clear();
    }

    private String[] listBundlesFromDir(final File dir) {
        try {
            final File bundleInstallerPath = getFile(_bc.getBundle()).getCanonicalFile();
            String[] list = dir.list(new FilenameFilter() {
				public boolean accept(File dir, String name) {
		            JarFile jf = null;
                    try {
                        File f = new File(dir.getPath() + File.separator + name).getCanonicalFile();
                        if (f.isDirectory()) {
                            return false;
                        }

                        // Ignore files which don't end with "?.ar" (that is: .jar, .war, .sar, .par)
                        int len = name.length();
                        if (!(len > 4 && name.endsWith("ar") && name.charAt(len - 4) == '.')) {
                            return false;
                        }

                        // Don't run into circles ... ignore ourself
                        if (f.equals(bundleInstallerPath)) {
                            return false;
                        }

                        jf = new JarFile(f);
                        
                        // ignore bundle without any bsn
                        String bsn = jf.getManifest().getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
                        if (bsn == null) {
                        	log(Level.WARN, "Ignoring %s: no Bundle-SymbolicName found", f);
                        	return false;
                        }

                        // Never load the felix framework                        
                        if (FELIX_SMB.equals(bsn)) {
                            return false;
                        }
                                               
                        // Never load a bundle which is declared with a -1 start level from the startlevel cfg file or if Bundle-StartLevel is found from bundle with value -1
                        String legacySL = jf.getManifest().getMainAttributes().getValue("Bundle-StartLevel");
                        if (_startLevels.getStartLevel(bsn, legacySL) == -1) {
                        	log(Level.DEBUG, "Ignoring %s: found negative Bundle-StartLevel", f);
                        	return false;
                        }

                        return true;
                    } catch (IOException e) {
                        log(Level.ERROR, e, "Could not read directory %s", dir);
                        return false;
                    } finally {
                    	try {
                    		if (jf != null) {
                    			jf.close();
                    		}
                    	} catch (IOException e) {                    		
                    	}
                    }
                }
            });

            return list;
        } catch (IOException e) {
            log(Level.ERROR, e, "Could not read directory %s", dir);
            throw new RuntimeException(e);
        }
    }

    private String getProperty(String name, String def) {
        String val = _bc.getProperty(name);
        if (val == null) {
            val = System.getProperty(name, def);
        }
        if (val == null) {
            val = def;
        }
        return val;
    }

    private List<String> getList(String propname, String def) {
        String csv = getProperty(propname, def);
        List<String> list = null;
        if (csv != null)
            for (String s : csv.split(",")) {
                s = s.trim();
                if (!"".equals(s)) {
                    if (list == null)
                        list = new Vector<String>();
                    list.add(s);
                }
            }
        return list;
    }

    private Filter getFilter(String propname) {
        String s = getProperty(propname, null);
        if (s == null) {
            return null;
        }
        if (s.startsWith("("))
            try {
                return FrameworkUtil.createFilter(s);
            } catch (Exception e) {
                log(Level.ERROR, e, "Could not create filter from %s", s);
                return null;
            }
        StringBuilder sb = new StringBuilder("(|");
        for (String bsn : getList(propname, null)) {
            sb.append("(").append(BUNDLE_SYMBOLICNAME).append("=").append(bsn).append(")");
        }
        sb.append(")");
        try {
            return FrameworkUtil.createFilter(sb.toString());
        } catch (Exception e) {
            log(Level.ERROR, e, "Could not create filter from %s", s);
            return null;
        }
    }

    private File[] getConfDirs(String propname, String defval) {
        File[] dirs;

        String dirPaths = getProperty(propname, defval);
        if (dirPaths != null) {
            StringTokenizer st = new StringTokenizer(dirPaths, ",: ");
            dirs = new File[st.countTokens()];
            int i = 0;
            while (st.hasMoreTokens()) {
                File dir = new File(st.nextToken().trim());
                dirs[i++] = dir;
            }
        } else {
            dirs = new File[0];
        }

        return dirs;
    }

    private boolean pathStartsWith(File f, File[] dirs) {
        for (int i = 0; i < dirs.length; i++) {
            if (pathStartWith(f, dirs[i]))
                return true;
        }
        return false;
    }

    private boolean pathStartWith(File f, File dir) {
        if (f.getAbsolutePath().startsWith(dir.getAbsolutePath())) {
            return true;
        }
        return false;
    }

    private boolean isFileInDir(File f, File dir) {
        return new File(dir.getPath() + File.separator + f.getName()).exists();
    }

    private boolean isOnlyInFirstDir(File f) {
        for (int i = 1; i < _deployDirs.length; i++)
            if (isFileInDir(f, _deployDirs[i]))
                return false;
        return true;
    }

    private boolean isFragment(Bundle b) {
        Dictionary<String, String> h = b.getHeaders();
        return h.get("Fragment-Host") != null;
    }

    private boolean isApplication(Bundle b) {
        String location = b.getLocation();
        if (location.endsWith(".par") || location.endsWith(".war") || location.endsWith(".sar")) {
            return true;
        }
        if (b.getEntry("/META-INF/mbeans-descriptors.xml") != null) {
            return true;
        }
        return false;
    }

    private String getSimpleName(Bundle b) {
        String path = b.getLocation();
        int i = path.lastIndexOf("/");
        if (i != -1) {
            int j = path.lastIndexOf("/", i - 1);
            if (j != -1) {
                path = path.substring(j + 1);
            } else {
                path = path.substring(i + 1);
            }
        }
        path += " (id#" + b.getBundleId() + ")";
        return path;
    }

    private DeployedBundle[] getDeployedBundles() {
        DeployedBundle[] array = new DeployedBundle[_deployedBundles.size()];
        array = (DeployedBundle[]) _deployedBundles.values().toArray(array);
        Arrays.sort(array, new DeployedBundleComparator());
        return array;
    }
    
	private Level parseLogLevel() {
		String logLevel = getProperty("bundleinstaller.debug", "WARN");
		logLevel = logLevel.trim();
		// the property may have the following values: ERROR, WARN, INFO, DEBUG, false (meaning INFO), true (meaning DEBUG)
		
		if (logLevel.equals("false")) {
			return Level.INFO;
		} else if (logLevel.equals("true")) {
			return Level.DEBUG;
		} else {
			try {
				return Level.valueOf(logLevel);
			} catch (Exception e) {
				return Level.DEBUG;
			}
		}
	}

    private void log(Level level, String format, Object ... args) {
    	log(level, null, format, args);
    }
    
    private void log(Level level, Throwable t, String format, Object ..._args) {
    	if (_logLevel.ordinal() >= level.ordinal()) {
    		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,S");
    		PrintStream ps = (level.ordinal() <= Level.WARN.ordinal()) ? err : out;
            StringBuilder sb = new StringBuilder();
            String msg = String.format(format, _args);
            sb.append(level + " - " + sdf.format(new Date()) + " - " + Thread.currentThread().getName() + " - " + msg);
            if (t != null) {
                sb.append("\n");
                sb.append(parse(t));
            }
    		ps.println(sb.toString());
    	}
    }

    private String parse(Throwable e) {
        StringWriter buffer = new StringWriter();
        PrintWriter pw = new PrintWriter(buffer);
        e.printStackTrace(pw);
        return (buffer.toString());
    }

    // --------------------------- Inner classes -----------------------------------------------

    /**
     * Utility class to store info about a deployed bundle.
     */
    class DeployedBundle {
        /** Our Bundle location */
        String _location;

        /** Our bundle file */
        File _file;

        /** Our bundle */
        Bundle _bundle;

        /** The last time we updated the bundle */
        long _lastModified;

        /** Tells if we have tried to start */
        boolean _started;

        public DeployedBundle(File f) {
            _location = "file:" + f.getPath();
            _file = f;
            _lastModified = _file.lastModified();
        }

        public DeployedBundle(Bundle b) throws MalformedURLException {
            _location = b.getLocation();
            _file = BundleInstallerImpl.this.getFile(b);
            _bundle = b;
            _lastModified = _file.lastModified();
            stop();
        }

        public boolean install() {
            try {
                _bundle = _bc.installBundle("reference:" + _location);
                _lastModified = _file.lastModified();
                log(Level.INFO, "Installed %s", getSimpleName(_bundle));
                return true;
            } catch (Throwable t) {
                log(Level.WARN, t, "Could not install %s", _file.getPath());
                return false;
            }
        }

        public void start() throws BundleException {
            if (!_started && isStartable()) {
                switch (_bundle.getState()) {
                case Bundle.INSTALLED:
                case Bundle.RESOLVED:
                	log(Level.DEBUG, "Starting %s", this);
                    _bundle.start(Bundle.START_TRANSIENT);
                    _started = true; // We 'll try to restart the bundle at next poll
                    break;
                }
            }
        }

        public void stop() {
            if (!isFragment(_bundle)) {
                try {
                    _started = false;
                    if (DEBUG) {
                        log(Level.DEBUG, "Stopping %s", _bundle);
                    }
                    _bundle.stop(Bundle.STOP_TRANSIENT);
                } catch (BundleException e) {
                    log(Level.WARN, e, "Could not stop %s", this);
                }
            }
        }

        public boolean needsUpdate() {
            // Either the bundle is newer, or has been replaced by a previous version.
            return _file.lastModified() != _lastModified;
        }

        public boolean update() {
            try {
                stop();
                log(Level.INFO, "Updating %s", getSimpleName(_bundle));
                _bundle.update();

                _lastModified = _file.lastModified();
                return true;
            } catch (Throwable e) {
                log(Level.WARN, e, "Could not update %s", getSimpleName(_bundle));
                return false;
            }
        }

        public void uninstall() {
            _lastModified = -1;
            log(Level.INFO, "Uninstalling %s", getSimpleName(_bundle));
            try {
                _started = false;
                _bundle.uninstall();
            } catch (Throwable e) {
                log(Level.WARN, e, "Could not uninstall %s", getSimpleName(_bundle));
            }
            _bundle = null;
        }

        @Override
        public String toString() {
            return getSimpleName(_bundle);
        }

        public File getFile() {
            return _file;
        }

        public Bundle getBundle() {
            return _bundle;
        }

        private boolean isStartable() {
            if (isFragment(_bundle)) {
                return false;
            }

            boolean startable = false;
            Dictionary<String, String> h = _bundle.getHeaders();

            if (_autoStartApps) {
                startable = true;
            } else {
                if (isApplication(_bundle)) {
                    // It's a PAR or a WAR: start only if deployed through our
                    // API or if PAR is in autostart
                    // mode.
                    boolean autoStartable = "true".equalsIgnoreCase((String) h.get("Bundle-AutoStart"));
                    if (autoStartable) {
                        startable = true;
                    }
                } else {
                    // It's a bundle: if the bundle has a "Bundle-AutoStart:false" header, then only start
                    // if the bundle
                    // has been deployed through our API.
                    String bundleAutoStart = (String) h.get("Bundle-AutoStart");
                    if (bundleAutoStart != null) {
                        if (bundleAutoStart.equalsIgnoreCase("true")) {
                            startable = true;
                        }
                    } else {
                        // The bundle don't have a Bundle-AutoStart header: only start it if it has an
                        // Activator, or if there is a
                        // SCR descriptor.
                        if (h.get(BUNDLE_ACTIVATOR) != null // OSGi Activator
                            || h.get("Service-Component") != null // Declarative Service
                            || _bundle.getEntryPaths("META-INF/services") != null // SPI
                            || h.get("DependencyManager-Component") != null) // DependencyManager
                        {
                            startable = true;
                        }
                    }
                }
            }

            log(Level.DEBUG, "Bundle %s is startable ? %s", _bundle.getSymbolicName(), startable);

            if (startable) {
                if ((_whiteList != null || _blackList != null) && BundleInstallerImpl.this.isOnlyInFirstDir(_file)) {
                	log(Level.DEBUG, "New bundle %s in %s is not subject to whitelist/blacklist",
                			_bundle.getSymbolicName(),
                			_firstDir.getName());
                	return true;
                }

                if (_blackList != null && _blackList.match(_bundle.getHeaders())) {
                	log(Level.DEBUG, "Bundle %s is blacklisted. Not started", _bundle.getSymbolicName());
                    return false;
                }

                if (_whiteList != null) {
                    if (_whiteList.match(_bundle.getHeaders())) {
                        return true;
                    } else {
                    	log(Level.DEBUG, "Bundle %s does not match whitelist. Not started", _bundle.getSymbolicName());
                        return false;
                    }
                }

                return true;
            }

            return false;
        }
    }

    class DeployedBundleComparator implements Comparator<DeployedBundle> {
        public int compare(DeployedBundle dp1, DeployedBundle dp2) {
            int myStartLevel = getStartLevel(dp1.getBundle());
            int otherStartLevel = getStartLevel(dp2.getBundle());
            int result = myStartLevel < otherStartLevel ? -1 : ((myStartLevel == otherStartLevel) ? 0 : 1);
            return result;
        }

        private int getStartLevel(Bundle b) {
            return _startLevels.getStartLevel(b);
        }
    }

    class Refresher {
        private final Runnable _callback;

        Refresher(Runnable callback) {
            _callback = callback;
        }

        void refresh() {
            log(Level.INFO, "Refreshing bundles ...");
            FrameworkWiring fw = _bc.getBundle(0).adapt(FrameworkWiring.class);
            fw.refreshBundles(null, new FrameworkListener() {
                public void frameworkEvent(FrameworkEvent event) {
                    switch (event.getType()) {
                    case FrameworkEvent.PACKAGES_REFRESHED:
                    	log(Level.DEBUG, "Bundles refreshed.");
                        _callback.run();
                        break;
                    case FrameworkEvent.ERROR:
                        log(Level.WARN, "Could not refresh bundle %s", getSimpleName(event.getBundle()));
                        break;
                    }
                }
            });
        }
    }
}
