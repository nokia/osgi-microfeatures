package com.alcatel.as.service.config.impl.file;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationPlugin;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import com.alcatel.as.service.concurrent.SerialExecutor;

/**
 * This is our file based configuration service. Mainly:
 * <ul>
 * <li>we periodically scan properties from property files
 * <li>we provide the resulting Dictionary objects into the OSGi registry with a service.pid
 * <li>we provide the Dictionary object itself into the Config Admin with same service.pid
 * </ul>
 */
public class StandaloneConfig implements EventHandler
{
    static final Logger _logger = Logger.getLogger("as.service.configuration.file");
    protected volatile Map<String, ConfigEntry> _configs = new ConcurrentHashMap<String, ConfigEntry>();
    protected volatile BundleContext _bc;
    protected volatile ConfigurationPlugin _cmPlugin; // possibly null object
    private String _confdir;
    private int _period = -1;
    private String _log4jpid;
    private Thread _thread;
    private ConfigurationAdmin _cm;

    /**
     * Creates our configuration service.
     * @param dir the directory when property files are stored
     * @param period the period we use, when tracking prop file updates.
     */
    public StandaloneConfig(String dir, String period)
    {
        this(dir, period, null);
    }

    public StandaloneConfig(String dir, String period, String log4jpid)
    {
        _confdir = (dir != null ? dir : "/tmp/confdir");
        _log4jpid = (log4jpid != null ? log4jpid : "log4j");
        
        if (!new File(_confdir).isDirectory())
        	throw new IllegalArgumentException(_confdir + " is not a valid configuration directory");

        //use basic log4j init until log4j config is provided
        //org.apache.log4j.BasicConfigurator.configure();

        if (period != null)
            try
            {
                _period = Integer.parseInt(period);
            }
            catch (Exception e)
            {
                _logger.warn("Configuration error: invalid refresh period:" + period, e);
            }
        if (_logger.isDebugEnabled())
            _logger.debug("new StandaloneConfig(dir=" + _confdir + ", period=" + _period + ")");
    }

	public void setBundleContext(BundleContext ctx) {
		_bc = ctx;
	}

	public void setConfigAdmin(ConfigurationAdmin cm) {
		_cm = cm;
	}
	
	public void setConfigPlugin(ConfigurationPlugin cmPlugin) {
		_cmPlugin = cmPlugin;
	}

    /**
     * Starts our service. All required dependencies are satisfied, and we'll be invoked in our
     * add/remove MBD methods.
     */
    protected synchronized void start()
    {
        // init ConfigEntries with directory content
    	
    	FilenameFilter filter = (File dir, String name) -> name.endsWith(ConfigEntry.CONFIG_EXT) && !name.startsWith(".#");    	    
		for (String file : new File(_confdir).list(filter)) {
			String pid = file.substring(0, file.lastIndexOf('.'));
			_logger.info("Creating conf for pid " + pid);
			_configs.put(pid, new ConfigEntry(_bc, pid, _confdir, _cm, _cmPlugin));
		}
		
		// load configuration
		doScan();

		// schedule config update checker thread
		if (_period > 0) {
			_thread = new Thread("ConfThread") {
				public void run() {					
					// initialize confdir watcher
					WatchService watcher = null;
					try {
						watcher = FileSystems.getDefault().newWatchService();
						File confdirFile = new File(_confdir);
						if (confdirFile.exists()) {
							Path confdirPath = confdirFile.toPath();
							confdirPath.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
						} else {
							_logger.error("Could not create conf directory watcher (confir not found: " + _confdir + ")");
							return;
						}
	            	} catch (Exception e) {
						_logger.error("Could not create conf directory watcher (" + _confdir + ")", e);
	            		return;
	            	}
					
					WatchKey watchKey = null;
					while (!isInterrupted()) {
						try {
	                    	// poll for conf directory changes.
							watchKey = watcher.take();
	        				_logger.debug("conf dir watcher wakeup");
							List<WatchEvent<?>> events = watchKey.pollEvents();
							if (events.size() > 0) {
								checkConfUpdates(filter);
							}
						} catch (InterruptedException e) {	
							break;
						} catch (Throwable t) {
							_logger.error("Caught unexpected exception when scanning for property file updates", t);
						} finally {
							if (watchKey != null) {
								watchKey.reset();
							}
						}
					}
					
					if (watcher != null) {
						try {
							watcher.close();
						} catch (IOException e1) {
						}
					}
				}
			};

			_thread.start();
		}
    }
    
    private synchronized void checkConfUpdates(FilenameFilter filter) {
		_logger.debug("checking configuration update");
		
		// check for new files
		for (String file : new File(_confdir).list(filter)) {
			String pid = file.substring(0, file.lastIndexOf('.'));
			if (_configs.get(pid) == null) {
				_logger.info("Creating conf for pid " + pid);
				_configs.put(pid, new ConfigEntry(_bc, pid, _confdir, _cm, _cmPlugin));
			}
		}
		// check for removed files
		List<ConfigEntry> toRemove = new ArrayList<>();
		Set<Map.Entry<String, ConfigEntry>> entries = _configs.entrySet();
		for (Map.Entry<String, ConfigEntry> e : entries) {
			String pid = e.getKey();
			ConfigEntry confEntry = e.getValue();
			if (!confEntry.fileExists()) {
				toRemove.add(confEntry);
			}
		}
		toRemove.forEach(confEntry -> {
			_logger.info("Removing conf for pid " + confEntry.getPid());
			_configs.remove(confEntry.getPid());
			confEntry.close();
		});

		// scan if some properties were modified
		doScan();    	
    }

    protected void propertiesUpdated(String pids) {
      for (String pid : pids.split(",")) 
      {
        ConfigEntry conf = _configs.get(pid.trim());
        if (conf != null)
        {
          conf.update();
        }
        else _logger.error("No config found for updated pid "+pid.trim(), new RuntimeException());
      }
    }

    /**
     * Stops our service.
     */
    protected synchronized void stop()
    { // invoked by DM thread
        if (_thread != null)
        {
          _thread.interrupt();
          try
          {
            _thread.join();
          }
          catch (InterruptedException e)
          {
          }
        }
    }

    /**
     * Periodically scans all registered MBD and eventually updates the associated dictionary.
     */
    protected synchronized void doScan()
    { // may be called from our run() method, or from DM
        try
        {
            // always provide log4j configuration first. If not, we might miss some debug logs.
            for (ConfigEntry config : _configs.values())
            {
                if (_log4jpid.equals(config.getPid()) || "org.ops4j.pax.logging".equals(config.getPid()))
                {
                    if (config.needsUpdate())
                    {
                        config.update();
                    }
                    break;
                }
            }

            // next, update other pids. // TODO the log4j pid should not be hard coded
            for (ConfigEntry config : _configs.values())
            {
                if (!"log4j".equals(config.getPid()) && config.needsUpdate())
                {
                    config.update();
                }
            }
        }
        catch (Throwable t)
        {
            _logger.warn("Error parsing configuration files", t);
        }
    }

	@Override
	public void handleEvent(Event event) {
		Object pid = event.getProperty("config.pid");
		if (pid != null) {
			if (_logger.isDebugEnabled()) {
				_logger.debug("Handling event: " + event);
			}
			if (pid instanceof String) {
				handleEvent((String) pid);
			} else if (pid instanceof String[]) {
				for (String p : (String[]) pid) {
					handleEvent(p);
				}
			} else {
				_logger.warn("Event contains a pid but with an invalid type: " + pid.getClass().getName());
			}
		} else {
			_logger.warn("Missing config.pid property in event " + event);
		}
	}

	private synchronized void handleEvent(String pid) {
		ConfigEntry conf = _configs.get(pid);
		if (conf != null) {
			if (_logger.isDebugEnabled()) {
				_logger.debug("Updating configuration pid " + pid);
			}
			try {
				conf.update();
			} catch (Exception e) {
				_logger.warn("Error while updating configuratio pid " + pid, e);
			}
		}
	}
}
