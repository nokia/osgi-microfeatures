package com.alcatel.as.service.diagnostics.impl;

import java.io.IOException;
import java.util.Dictionary;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;

/**
 * Describes a component. Component declarations form descriptions of components
 * that are managed by the dependency manager. They can be used to query their state
 * for monitoring tools. The dependency manager shell command is an example of
 * such a tool.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface ComponentDeclaration {
    /** Names for the states of this component. */
    public static final String[] STATE_NAMES = { "unregistered", "registered" };
    /** State constant for an unregistered component. */
    public static final int STATE_UNREGISTERED = 0;
    /** State constant for a registered component. */
    public static final int STATE_REGISTERED = 1;
    /** Returns a list of dependencies associated with this component. 
     * @throws InvalidSyntaxException 
     * @throws IOException */
    public ComponentDependencyDeclaration[] getComponentDependencies() throws IOException, InvalidSyntaxException;
    /** Returns the description of this component (the classname or the provided service(s)) */
    public String getName();
    /** Returns the state of this component. */
    public int getState();
    /** Returns the bundle of this component. */
    public Bundle getBundle();
}
