// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.agent;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Properties;

import alcatel.tess.hometop.gateways.utils.IntHashtable;

/**
 * The Mux Handler descriptors informations (protocol, elasticity, flags, etc ...) 
 */
public class MuxHandlerDesc {
	public final static String CLASS = "class";
	public final static String APP_NAME = "appName";
	public final static String APP_ID = "appId";
	public final static String PROTOCOL = "protocol";
	public final static String HIDDEN = "hidden";
	public final static String AUTO_REPORTING = "autoreporting";
	public final static String ELASTIC = "elastic";
	public final static String FLAGS = "flags";

	private String _classname, _appName, _protocol;
	private int _appId;
	private IntHashtable _flags;
	private final boolean _hidden;
	private final boolean _elastic;
	private final boolean _autoReport; // true by default
	
	@SuppressWarnings("rawtypes")
	public MuxHandlerDesc(Dictionary desc, MuxHandlerDesc defDesc) {
		this(desc, defDesc, null);
	}

	@SuppressWarnings("rawtypes")
	public MuxHandlerDesc(Dictionary desc, MuxHandlerDesc defDesc, Properties flagsProp) {
		// parse protocol (required)
		_protocol = (String) desc.get(PROTOCOL);
		if (_protocol == null) {
			if (defDesc != null) {
				_protocol = defDesc.getProtocol();
			} else {
				throw new IllegalArgumentException("Invalid MuxHandler Description (missing protocol)");
			}
		}
		
		// parse appName (required)
		_appName = (String) desc.get(APP_NAME);
		if (_appName == null) {
			if (defDesc != null) {
				_appName = defDesc.getAppName();
			} /*else {
				throw new IllegalArgumentException("Invalid MuxHandler Description (missing appName)");
			}*/
		}		
		
		// parse appId (required)
		Object appId = desc.get(APP_ID);
		if (appId != null) {					
			try {
				_appId = Integer.parseInt(appId.toString());
			} catch (Exception e) {
				throw new IllegalArgumentException("Invalid MuxHandler Description (bad appId)");
			}
		} else if (defDesc != null) {
			_appId = defDesc.getAppId();
		} /* else {
			throw new IllegalArgumentException("Invalid MuxHandler Description (missing appId)");
		}*/

		// parse class name (optional)
		_classname = (String) desc.get(CLASS); 
		if (_classname == null && defDesc != null) {
			_classname = defDesc.getClassName();
		}
		
		// parse hidden flag (optional, false by default)
		Object hidden = (String) desc.get(HIDDEN);
		if (hidden != null) {
			_hidden = Boolean.valueOf(hidden.toString());
		} else if (defDesc != null) {
			_hidden = defDesc.isHidden();
		} else {
			_hidden = false;
		}
		
		// parse elastic flag (optional, false by default)
		Object elastic = desc.get(ELASTIC);
		if (elastic != null) {
			_elastic = Boolean.valueOf(elastic.toString());
		} else if (defDesc != null) {
			_elastic = defDesc.isElastic();
		} else {
			_elastic = false;
		}
		
		// parse autoReporting flag (optional, true by default)
		Object autoReport = (String) desc.get(AUTO_REPORTING);
		if (autoReport != null) {
			_autoReport = Boolean.valueOf(autoReport.toString());
		} else if (defDesc != null) {			
			_autoReport = defDesc.autoReport();
		} else {
			_autoReport = true;
		}
		
		// parse flags (optional)
		if (flagsProp == null) {
			flagsProp = (Properties) desc.get(FLAGS);
		}
		
		if (flagsProp != null) {
			_flags = new IntHashtable();

			Enumeration enumer = flagsProp.keys();
			while (enumer.hasMoreElements()) {
				String key = (String) enumer.nextElement();
				String val = flagsProp.getProperty(key);
				int i = 0;
				if (key.startsWith("0x") || key.startsWith("0X")) {
					i = Integer.parseInt(key.substring(2), 16);
				} else {
					i = Integer.parseInt(key);
				}
				_flags.put(i, val.trim());
			}
		} else if (defDesc != null) {
			_flags = defDesc.getFlags();
		}
	}
	
	public String getClassName() {
		return _classname;
	}

	public int getAppId() {
		return _appId;
	}

	public String getAppName() {
		return _appName;
	}

	public String getProtocol() {
		return _protocol;
	}

	public IntHashtable getFlags() {
		return _flags;
	}

	public boolean isHidden() {
		return _hidden;
	}

	public boolean isElastic() {
		return _elastic;
	}

	public boolean autoReport() {
		return _autoReport;
	}
	
	@Override
	public String toString() {
		StringBuilder buff = new StringBuilder();
		buff.append("MuxHandlerDesc [");
		buff.append("class=").append(_classname);
		buff.append(", appId=").append(_appId);
		buff.append(", appName=").append(_appName);
		buff.append(", protocol=").append(_protocol);
		buff.append(", isElastic=").append(_elastic);
		buff.append(", isHidden=").append(_hidden);
		buff.append(", autoReport=").append(_autoReport);
		if (_flags != null) {
			buff.append(", flags=").append(_flags);
		}
		buff.append(']');
		return buff.toString();
	}	
}
