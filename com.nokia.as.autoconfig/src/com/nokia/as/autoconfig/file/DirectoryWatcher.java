package com.nokia.as.autoconfig.file;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;

import com.alcatel.as.service.log.LogService;
import com.nokia.as.autoconfig.AutoConfigurator;

import alcatel.tess.hometop.gateways.utils.Log;

public class DirectoryWatcher extends Thread {

	private LogService logger = Log.getLogger(AutoConfigurator.LOGGER);
	private final WatchService directoryWatcher;
	private final Map<WatchKey, Path> keys = new HashMap<>();
	private final AutoConfigurator autoconf;
	private String path;
	
	public DirectoryWatcher(AutoConfigurator autoconf, String path) throws IOException {
		this.autoconf = autoconf;
		this.path = path;
		this.directoryWatcher = FileSystems.getDefault().newWatchService();
		registerDir(Paths.get(path));
	}
	
	private void registerDir(Path dir) throws IOException {
		logger.debug("DirectoryWatcher will watch %s", dir.toString());
		keys.put(dir.register(directoryWatcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY), dir);
	}
	
	@Override
	public void run() {
		while(true) {
			WatchKey key;
			try  {
				key = directoryWatcher.take();
				Thread.sleep(500); //trying to avoid double updates
			} catch (InterruptedException e) {
				logger.debug("Interrupted watching directory %s", e, path);
				return;
			}
			
			Path dir = keys.get(key);
			if(dir == null) {
				logger.warn("Error while watching directory %s: WatchKey not recognized", path);
				continue;
			}
			
			key.pollEvents().stream()
							.forEach(this::consume);
			
			key.reset();
		}
	}
	
	private void consume(WatchEvent<?> event) {
		autoconf.fileEvent(path);
	}
	
}
