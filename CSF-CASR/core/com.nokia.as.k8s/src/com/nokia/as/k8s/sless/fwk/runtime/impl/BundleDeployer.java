package com.nokia.as.k8s.sless.fwk.runtime.impl;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.ServiceDependency;
import org.apache.log4j.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import com.alcatel.as.service.concurrent.SerialExecutor;
import com.nokia.as.k8s.sless.Function;

/**
 * Class used to deploy/undeploy OSGi bundles.
 */
public class BundleDeployer {
	
	private final static String BUNDLE_REGISTRY = "bundles.registry";
	private final static String REG_INSTALLED_BUNDLES = "bundles.installed";

	/**
	 * A deployable module
	 */
	public static class Module {
		/**
		 * The location identifier of the bundle to install. The specified location
		 * identifier will be used as the identity of the bundle. Every installed bundle
		 * is uniquely identified by its location identifier which is typically in the
		 * form of a URL.
		 */
		public final String location;

		/**
		 * The InputStream object from which this bundle will be read
		 */
		public final InputStream input;

		/**
		 * Creates a deployable module.
		 * 
		 * @param location The location identifier of the bundle to install. The
		 *                 specified location identifier will be used as the identity of
		 *                 the bundle. Every installed bundle is uniquely identified by
		 *                 its location identifier which is typically in the form of a
		 *                 URL.
		 * @param input    The InputStream object from which this bundle will be read
		 */
		Module(String location, InputStream input) {
			this.location = location;
			this.input = input;
		}
	}

	private final static Logger _log = Logger.getLogger(BundleDeployer.class);
	private final BundleContext _bctx;
	private final DependencyManager _dm;
	private final Map<String, AtomicInteger> _refCount = new HashMap<>();
	private final SerialExecutor _serial = new SerialExecutor(_log);
	private final Properties _registry;

	public BundleDeployer(BundleContext ctx) {
		_bctx = ctx;
		_dm = new DependencyManager(ctx);
		_registry = getBundleRegistry();
		uninstallPreviousFunctions(_registry); 
	}

	/**
	 * Deploys a list of modules
	 * 
	 * @param context
	 * @param modules the list of bundles to deploy.
	 * @return the list of deployed bundles
	 */
	public CompletableFuture<List<Bundle>> deploy(List<Module> modules) throws Exception {
		CompletableFuture<List<Bundle>> completed = new CompletableFuture<>();
		_serial.execute(() -> doDeploy(completed, modules));
		return completed;
	}
	
	/**
	 * Undeploy a list of already installed bundles.
	 * 
	 * @param bundles
	 * @return a CF which completes once the bundles are uninstalled
	 */
	public CompletableFuture<Void> undeploy(List<Bundle> bundles) {
		// for now, simply stop/uninstall the bundles. Later, we may implement a
		// stronger stragety which refreshes the framework in a separate thread
		CompletableFuture<Void> cf = new CompletableFuture<>();
		_serial.execute(() -> {
			doUndeploy(bundles);
			cf.complete(null);
		});
		return cf;
	}

	/**
	 * Tracks a Function service
	 */
	@SuppressWarnings("unused")
	public Object track(Consumer<Function> added, Consumer<Function> removed, String filter) {
		Object callback = new Object() {
			void added(Function f) {
				added.accept(f);
			}

			void removed(Function f) {
				removed.accept(f);
			}
		};
		Component functionComp = _dm.createComponent().setImplementation(callback);
		ServiceDependency dep = _dm.createServiceDependency().setService(Function.class, filter).setRequired(true)
				.setCallbacks("added", "removed");
		functionComp.add(dep);

		_dm.add(functionComp);
		return functionComp;
	}

	private void doDeploy(CompletableFuture<List<Bundle>> completed, List<Module> modules) {
		List<Bundle> deployed = new ArrayList<>(0);

		// Install all bundles
		for (Module m : modules) {
			try {
				// Update bundle registry
				_registry.put(m.location, "true");
				storeBundleRegistry(_registry);
				
				// install the bundle, or return the bundle if it is already installed.
				Bundle b = _bctx.installBundle(m.location, m.input);
				
				// increment bundle ref count
				incBundleRefCount(b.getLocation());

				// Register this bundle in the list of bundles to start (see after this loop)
				deployed.add(b);
			} catch (BundleException e) {
				doUndeploy(deployed);
				completed.completeExceptionally(e);
				return;
			}
		}

		// Next, start all bundles
		for (Bundle b : deployed) {
			try {
				// Start the bundle. If the bundle was already started, the method has no effect.
				b.start();
			} catch (Exception e) {
				undeploy(deployed);
				completed.completeExceptionally(e);
				return;
			}
		}
		completed.complete(deployed);
	}

	private void incBundleRefCount(String location) {
		AtomicInteger count = _refCount.computeIfAbsent(location, (key) -> new AtomicInteger());
		count.incrementAndGet();
	}
	
	/**
	 * Decrements the refcount for a given bundle location
	 * Returns true if the location is not referenced anymore, else false.
	 */
	private boolean decBundleRefCount(String location) {
		AtomicInteger count = _refCount.get(location);
		if (count == null) {
			return true; // unexpected, but return true to indicate the bundle location is not refered anymore 
		}
		if (count.decrementAndGet() == 0) {
			_refCount.remove(location);
			return true;
		}
		return false;
	}

	/**
	 * Untrack a Function servce
	 */
	public void untrack(Object function) {
		Component c = (Component) function;
		DependencyManager dm = c.getDependencyManager();
		dm.remove(c);
	}

	private void doUndeploy(List<Bundle> bundles) {
		List<Bundle> toUninstall = new ArrayList<>();
		for (Bundle b : bundles) {
			if (decBundleRefCount(b.getLocation())) {
				try {
					toUninstall.add(b);
					b.stop();
				} catch (BundleException e) {
					_log.warn("could not stop bundle " + b.getLocation(), e);
				}
			}
		}
		for (Bundle b : toUninstall) {
			try {
				b.uninstall();
			} catch (BundleException e) {
				_log.warn("could not uninstall bundle " + b.getLocation(), e);
			}
			_registry.remove(b.getLocation());			
		}
		storeBundleRegistry(_registry);
	}
	
	private Properties getBundleRegistry() {
		Properties registry = new Properties();
        File f = _bctx.getDataFile(BUNDLE_REGISTRY);
        try (DataInputStream in = new DataInputStream(new FileInputStream(f))) {
            registry.load(in);
            return registry;
        } catch (Throwable ignored) {
            return registry;
        }
	}
	
	private void storeBundleRegistry(Properties registry) {
        File f = _bctx.getDataFile(BUNDLE_REGISTRY);
        try (DataOutputStream dout = new DataOutputStream(new FileOutputStream(f))) {
            if (_log.isDebugEnabled()) _log.debug("storing registry " + registry);
            registry.store(dout, "Serverless Loaded Functions");
        } catch (Throwable t) {
            _log.warn("Could not store servleless bundle registry", t);
        }
	}
	
	private void uninstallPreviousFunctions(Properties registry) {
		runActionOnInstalledBundles(registry, (bundle) -> {
	    	  try {
	    		  bundle.stop();
	    	  } catch (Exception e) {
	    		  _log.warn("Could not stop bundle " + bundle.getLocation(), e);
	    	  }
		});
		
		runActionOnInstalledBundles(registry, (bundle) -> {
	    	  try {
	    		  _log.info("Uninstall " + bundle.getLocation());
	    		  bundle.uninstall();
	    	  } catch (Exception e) {
	    		  _log.warn("Could not uninstall bundle " + bundle.getLocation(), e);
	    	  }
		});
		registry.clear();
		storeBundleRegistry(registry);
	}

	private void runActionOnInstalledBundles(Properties registry, Consumer<Bundle> action) {
		Stream.of(_bctx.getBundles())
	      .filter(b -> registry.get(b.getLocation()) != null)
	      .forEach(action);	      
	}
}
