// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.diagnostics.impl;

/**
 * Describes a component dependency. They form descriptions of dependencies
 * that are managed by the dependency manager. They can be used to query their state
 * for monitoring tools. The dependency manager shell command is an example of
 * such a tool.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface ComponentDependencyDeclaration {
    /** Names for the states of this dependency. */
    public static final String[] STATE_NAMES = { 
        "optional unavailable", 
        "optional available", 
        "required unavailable", 
        "required available",
        "optional (not tracking)",
        "required (not tracking)"
        };
    /** State constant for an unavailable, optional dependency. */
    public static final int STATE_UNAVAILABLE_OPTIONAL = 0;
    /** State constant for an available, optional dependency. */
    public static final int STATE_AVAILABLE_OPTIONAL = 1;
    /** State constant for an unavailable, required dependency. */
    public static final int STATE_UNAVAILABLE_REQUIRED = 2;
    /** State constant for an available, required dependency. */
    public static final int STATE_AVAILABLE_REQUIRED = 3;
    /** State constant for an optional dependency that has not been started yet. */
    public static final int STATE_OPTIONAL = 4;
    /** State constant for a required dependency that has not been started yet. */
    public static final int STATE_REQUIRED = 5;
    /** Returns the name of this dependency. */
    public String getName();
    /** Returns the name of the type of this dependency. */
    public String getType();
    /** Returns the state of this dependency. */
    public int getState();
}
