// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh.engine;

import java.io.PrintStream;

import java.util.*;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.osgi.framework.*;
    
public class IOHCommand {
    
    protected String _scope, _name, _desc;
    protected IOHCommand (String scope, String name, String desc){
	_scope = scope;
	_name = name;
	_desc = desc;
    }
    public String getScope() { return _scope; }
    public String getName() { return _name; }
    public String getUsage() { return _desc; }
    public String getShortDescription() { return _desc; }
    public void execute(String commandLine, PrintStream out, PrintStream err) {}
    public void execute (CommandSession session, String... args){}

    public void register (BundleContext ctx){
	Hashtable t = new Hashtable();
	t.put (CommandProcessor.COMMAND_SCOPE, getScope ());
	t.put (CommandProcessor.COMMAND_FUNCTION, getName ());
	ctx.registerService (Object.class.getName(), this, t);
    }

}
