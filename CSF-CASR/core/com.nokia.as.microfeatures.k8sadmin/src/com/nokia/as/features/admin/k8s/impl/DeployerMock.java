package com.nokia.as.features.admin.k8s.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Init;
import org.apache.felix.dm.annotation.api.LifecycleController;

import com.nokia.as.features.admin.K8sFeaturesServlet;
import com.nokia.as.features.admin.k8s.DeployStatus;
import com.nokia.as.features.admin.k8s.Deployer;
import com.nokia.as.features.admin.k8s.Function;
import com.nokia.as.features.admin.k8s.Pod;
import com.nokia.as.features.admin.k8s.Route;
import com.nokia.as.features.admin.k8s.Runtime;

@Component
public class DeployerMock implements Deployer {	
	
	@LifecycleController
	Runnable _startme;
	
	private Map<String, Function> functionRepo = new HashMap<>();
	private Map<String, Route> routeRepo = new HashMap<>();
	private Map<String, Runtime> runtimeRepo = new HashMap<>();
	
	@Init
	void init() {
		if (Boolean.getBoolean("standalone")) {
			_startme.run();
			System.out.println("K8S admin ready (http://localhost:9090/features/index.html)");
		}
	}

	@Override
	public void deployRuntime(Runtime runtime) throws Exception {
		runtime.status(DeployStatus.DEPLOYED(3));
		runtime.addPod(new Pod("pod1", "127.0.0.1", 17001));
		runtime.addPod(new Pod("pod2", "127.0.0.2", 17001).ready(true));
		runtime.addPod(new Pod("pod3", "127.0.0.3", 17001).ready(true));
		runtimeRepo.put(runtime.name + "@" + K8sFeaturesServlet.CURRENT_NAMESPACE, runtime);
	}

	@Override
	public void undeployRuntime(String name) throws Exception {
		Runtime r = runtimeRepo.remove(name);
		r.status(DeployStatus.UNDEPLOYED);
	}

	@Override
	public void createFunction(Function function) throws Exception {
		functionRepo.put(function.name + "@" + K8sFeaturesServlet.CURRENT_NAMESPACE, function);
	}

	@Override
	public void deleteFunction(String name) throws Exception {
		functionRepo.remove(name);
	}

	@Override
	public void createRoute(Route route) throws Exception {
		routeRepo.put(route.name + "@" + K8sFeaturesServlet.CURRENT_NAMESPACE, route);
	}

	@Override
	public void deleteRoute(String name) throws Exception {
		routeRepo.remove(name);
	}

	@Override
	public List<Function> deployedFunctions() {
		return new ArrayList<>(functionRepo.values());
	}

	@Override
	public List<Runtime> deployedRuntimes() {
		return new ArrayList<>(runtimeRepo.values());
	}

	@Override
	public List<Route> deployedRoutes() {
		return new ArrayList<>(routeRepo.values());
	}

}
