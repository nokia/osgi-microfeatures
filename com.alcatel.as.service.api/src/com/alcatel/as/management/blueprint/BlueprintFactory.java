// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.management.blueprint;

import java.util.Set;
import java.io.InputStream;
import java.net.URL;

/**
 * requests the OBR for a list of bundles
 */
public interface BlueprintFactory {
    public static final String OBR_EXT = ".obr";

    /**
     * query the OBR and generate a Blueprint 
     * @param query a single ldap filter or a space separated list of bundle symbolic names to query from OBR
     */
    Blueprint generateBlueprint(String query) throws Exception ;
    /**
     * update an existing Blueprint
     * @param bp an existing Blueprint
     * @param query an optional single ldap filter or space separated list of bundle symbolic names to query from OBR
     */
    Blueprint updateBlueprint(Blueprint bp, String query) throws Exception ;
    /**
     * query the OBR to simply find the root resources, without performing any resolution
     * @param query a single ldap filter or a space separated list of bundle symbolic names to query from OBR
     */
    Set<URL> findResources(String query) throws Exception ;
    /**
     * query the OBR to simply find the root resources providing the given package
     * @param pkg a package name
     */
    Set<URL> findPackage(String pkg) throws Exception ;
    /**
     * load a previously serialized Blueprint
     */
    Blueprint loadBlueprint(InputStream in) throws Exception ;
    /**
     * updates the OBR with files from the dropzone directory
     */
    BlueprintFactory updateObr(String jarslist, String dropzone) throws Exception ;
    /**
     * point to a different OBR (includes a refresh)
     */
    BlueprintFactory setObrUrl(URL url) throws Exception ;
    /**
     * reloads the OBR after an update
     */
    BlueprintFactory refresh() throws Exception ;
}
