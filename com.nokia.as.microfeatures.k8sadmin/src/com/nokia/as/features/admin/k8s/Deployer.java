// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.features.admin.k8s;

import java.util.List;

public interface Deployer {
	public void deployRuntime(Runtime runtime) throws Exception;
	
	public void undeployRuntime(String name) throws Exception;
	
	public void createFunction(Function function) throws Exception;
	
	public void deleteFunction(String name) throws Exception;
	
	public void createRoute(Route route) throws Exception;
	
	public void deleteRoute(String name) throws Exception;
	
	public List<Function> deployedFunctions();
	
	public List<Runtime> deployedRuntimes();
	
	public List<Route> deployedRoutes();

}
