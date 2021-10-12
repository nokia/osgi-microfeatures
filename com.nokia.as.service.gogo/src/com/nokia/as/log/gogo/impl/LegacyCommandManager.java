// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.log.gogo.impl;

import static org.apache.felix.service.command.CommandProcessor.COMMAND_FUNCTION;
import static org.apache.felix.service.command.CommandProcessor.COMMAND_SCOPE;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.service.command.Descriptor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

/**
 * This component manages legacy / deprecated casr gogo commands.
 * A legacy gogo command can specify some alternate aliases for scope/functions, which won't be displayed by the gogo help command.
 */
@Component(provides = LegacyCommandManager.class)
@Property(name = COMMAND_SCOPE, value = "felix")
@Property(name = COMMAND_FUNCTION, value = { "help" })
@Property(name = Constants.SERVICE_RANKING, intValue = 10)
public class LegacyCommandManager {
	
	/**
	 * Map of commands. Key scope, value of set of commands objects (either a string, or an array of strings).
	 */
	private final Map<String, Set<Object>> _allCommands = new HashMap<>();
	
	/**
	 * Map of legacy commands, which are auto registered by our service
	 */
	private final Map<Object, ServiceRegistration<?>> _legacyCommands = new HashMap<>();
	
	/**
	 * Service property added to an hidden gogo command
	 */
	public final static String HIDE = "casr.osgi.command.scope.hide";
	
	/**
	 * Service property for aliased (deprecated) scopes
	 */
	public final static String COMMAND_SCOPE_ALIAS = "casr.osgi.command.scope.alias";
	
	/**
	 * Service property for aliased (deprecated) functions
	 */
	public final static String COMMAND_FUNCTION_ALIAS = "casr.osgi.command.function.alias";
	
	/**
	 * Standard line separator.
	 */
	private final static String LINE_SEP = System.getProperty("line.separator");
	
	/**
	 * Our OSGI Bundle Context
	 */
	@Inject
	private BundleContext _bc;
	
	/**
	 * Original gogo help function.
	 */
	@ServiceDependency(service = ServiceDependency.Any.class, filter = "(&(osgi.command.scope=felix)(!(service.ranking=*)))")
	Object _originalHelp;

	/**
	 * Register all gogo commands, except the deprecated commands that are registered by ourself. The hidden commands are tagged with
	 * a special "casr.osgi.command.scope.hide" service property (see bindLegacyCommand method).
	 */
	@ServiceDependency(service = ServiceDependency.Any.class, filter = "(&(" + COMMAND_SCOPE + "=*)(!(" + HIDE + "=true)))", removed = "unbindCommand")
	synchronized void bindCommand(Object command, Dictionary<String, Object> props) {
		String scope = (String) props.get(COMMAND_SCOPE);
		Object ofunc = props.get(COMMAND_FUNCTION);
		Set<Object> commands = _allCommands.computeIfAbsent(scope, (key) -> new HashSet<>());
		commands.add(ofunc);
	}
	
	synchronized void unbindCommand(Object command, Dictionary<String, Object> props) {
		String scope = (String) props.get(COMMAND_SCOPE);
		Object ofunc = props.get(COMMAND_FUNCTION);
		Set<Object> commands = _allCommands.get(scope);
		if (commands != null) {
			commands.remove(ofunc);
			if (commands.size() == 0) {
				_allCommands.remove(scope);
			}
		}
	}
	
	/**
	 * Legacy commands which we don't want to be displayed by help command.
	 */
	@ServiceDependency(required = false, service = ServiceDependency.Any.class, filter = "(" + COMMAND_SCOPE_ALIAS + "=*)", removed = "unbindLegacyCommand")
	void bindLegacyCommand(Object command, Dictionary<String, Object> props) {
		String scope = (String) props.get(COMMAND_SCOPE_ALIAS);
		Object ofunc = props.get(COMMAND_FUNCTION_ALIAS);
		if (ofunc == null) {
			ofunc = props.get(COMMAND_FUNCTION);
		}
		if (ofunc != null) {
			// copy service properties
			Hashtable<String, Object> commandProps = new Hashtable<>();
			Enumeration<String> keys = props.keys();
			while(keys.hasMoreElements()) {
				String key = keys.nextElement();
				commandProps.put(key,  props.get(key));
			}
			// set alias for scope
			commandProps.put(COMMAND_SCOPE, scope);
			commandProps.put(COMMAND_FUNCTION, ofunc);
			// remove alias service properties to avoid infinite recursion
			commandProps.remove(COMMAND_SCOPE_ALIAS);
			commandProps.remove(COMMAND_FUNCTION_ALIAS);
			// Add special property so this command won't be dislayed by our help command.
			commandProps.put(HIDE, "true");
			// Register legacy command (which won't be displayed by our help command)
			ServiceRegistration<?> reg = _bc.registerService(command.getClass().getName(), command, commandProps);
			synchronized (this) {
				_legacyCommands.put(command, reg);
			}
		}
	}
	
	void unbindLegacyCommand(Object command, Dictionary<String, Object> props) {
		ServiceRegistration<?> reg = null;
		synchronized (this) {
			reg = _legacyCommands.remove(command);
		}
		if (reg != null) {
			try {
				reg.unregister();
			} catch (Exception e) {}
		}
	}
	
	/**
	 * Overrides the defaulg gogo help command, in order to not display aliased deprecated commands.
	 */
	@Descriptor("displays available commands")
	public synchronized void help() {
		try {
			TreeSet<String> commands = new TreeSet<>();
			for (Map.Entry<String, Set<Object>> e : _allCommands.entrySet()) {
				String scope = e.getKey();
				for (Object ofunc : e.getValue()) {
					String[] funcs = (ofunc instanceof String[]) ? (String[]) ofunc : new String[] { String.valueOf(ofunc) };
					for (String func : funcs) {
						commands.add(scope + ":" + func);
					}
				}
			}
			
			StringBuilder sb = new StringBuilder();
			for (String cmd : commands) {
				sb.append(cmd);
				sb.append(LINE_SEP);
			}
			System.out.println(sb.toString());
		} catch (Exception e) {
			System.out.println(format("internal error", e));
		}
	}

	/**
	 * Proxy real gogo help for a specfic real command
	 */
	@Descriptor("displays information about a specific command")
	public Object help(@Descriptor("target command") String name) {
		try {
			Method m = _originalHelp.getClass().getDeclaredMethod("help", String.class);
			if (m == null) {
				return "could not find help command from class " + _originalHelp;
			}
			Object result = m.invoke(_originalHelp, name);
			return result != null ? result : null;
		} catch (Exception e) {
			return format("internal gogo error", e);
		}
	}

	private String format(String msg, Exception e) {
		StringWriter buffer = new StringWriter();
		PrintWriter pw = new PrintWriter(buffer);
		pw.println(msg);
		if (e != null) {
			e.printStackTrace(pw);
		}
		return (buffer.toString());
	}

}
