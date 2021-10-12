// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.agent;

import java.util.Hashtable;

import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;

import com.alcatel.as.service.management.ManagedService;
import com.alcatel.as.service.management.ManagedServiceSupport;
import com.alcatel.as.service.management.ManagementService;
import com.nextenso.agent.event.AsynchronousEvent;
import com.nextenso.agent.event.AsynchronousEventListener;
import com.nextenso.agent.event.AsynchronousEventScheduler;
import com.nextenso.mux.MuxHandler;
import com.nextenso.mux.event.MuxMonitor;
import com.nextenso.mux.event.MuxMonitorable;

// Legacy class. TODO: refactor !
@SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
public class MuxMonitorImpl implements MuxMonitor, AsynchronousEventListener {
    static final Logger _logger = Logger.getLogger("callout.MuxMonitorImpl");
    private static Hashtable _monitorables = new Hashtable();
    private static volatile BundleContext _bctx;

    public static void setBundleContext(BundleContext bctx) {
        _bctx = bctx;
    }
    
    private MuxHandler _handler;

    public MuxMonitorImpl() {
    }

    public MuxMonitorImpl(MuxHandler handler) {
        this._handler = handler;
    }

    //
    // Used when a MuxHandler wants to register a MuxMonitorable
    // 
    @Override
    public Object registerMonitorable(int appId, String appName, String appInstance, MuxMonitorable monitorable) {
        startReporter(_handler.getInstanceName(), appId, appName, appInstance, monitorable);
        return null;
    }

    //
    // Public method used to launch a reporter
    //
    public static void startReporter(final String parent, int appId, String applicationName, String applicationInstance,
        final MuxMonitorable monitorable)
    {
        String appName = applicationName;
        if (appName == null) {
            appName = monitorable.getClass().getName();
            int index = appName.lastIndexOf('.');
            if (index != -1)
                appName = appName.substring(index + 1);
        }
        String prefix = parent + "-";
        String appInstance = applicationInstance;
        if (appInstance.startsWith(prefix) == false) {
            appInstance = prefix + appInstance;
        }

        // Reporter instantiation is now externalized. Just register the ManagedService to the white
        // board
        final String $appName = appName;
        final String $appInstance = appInstance;

        Hashtable props = new Hashtable();
        props.put(ManagedService.COMPONENT_NAME, $appName);
        props.put(ManagedService.INSTANCE_NAME, $appInstance);
        props.put(ManagedService.COMPONENT_ID, "0");
        props.put(ManagedService.PARENT, parent);
        props.put(ManagedService.ENABLE_STATISTIC, "true");
        props.put(ManagedService.ENABLE_COMMAND, "true");
        ManagedService ms = new ManagedServiceSupport() {
            @Override
            public int[] getStatistics() {
                return monitorable.getCounters();
            }

            @Override
            public void adminCommandReceived(int code, String replyTo, int[] intParams, String[] strParams) {
                monitorable.commandEvent(code, intParams, strParams);
            }

            @Override
            public void commandReceived(int code, String replyTo, byte[] data, int off, int len) {
                monitorable.muxGlobalEvent(code, replyTo, data, off, len);
            }
        };
        _logger.debug("registering managed service for monitorable " + monitorable);
        _bctx.registerService(ManagedService.class.getName(), ms, props);

        try {
            synchronized (_monitorables) {
                _monitorables.put("I:" + appInstance, monitorable);
                MuxMonitorable[] tmp = (MuxMonitorable[]) _monitorables.get(appName);
                if (tmp == null) {
                    tmp = new MuxMonitorable[] { monitorable };
                } else {
                    MuxMonitorable[] clone = new MuxMonitorable[tmp.length + 1];
                    System.arraycopy(tmp, 0, clone, 0, tmp.length);
                    clone[clone.length - 1] = monitorable;
                    tmp = clone;
                }
                _monitorables.put(appName, tmp);
            }
        } catch (Exception e) {
            _logger.warn("Monitoring could not be started for " + appName + "(" + appInstance
                + ") : the Reporter could not be initialized", e);
        }
    }

    /***************************************************************************************************
     ********************************************* Events **********************************************
     ***************************************************************************************************/

    /**
     * Sends an event that will be broadcast to all JVMs.
     */
    @Override
    public boolean sendGlobalEvent(String appName, String appInstance, int identifierI, String identifierS, byte[] data,
        int off, int len)
    {
        return publishGlobalEvent(appName, appInstance, identifierI, identifierS, data, off, len);
    }

    public boolean publishGlobalEvent(String appName, String appInstance, final int identifierI,
        final String identifierS, final byte[] data, final int off, final int len)
    {
        // TODO ManagementService not supported anymore ?? what should we do ???
        ManagementService ms = null; // Services.getInstance().getManagementService();
        if (ms == null) {
            _logger.warn("can't publish mux global event: management service not supported");
            return false;
        }
        if (appName == null) {
            _logger.warn("Invalid muxGlobalEvent to send: null appName");
            return false;
        }
        if (appInstance == null) {
            _logger.warn("Invalid muxGlobalEvent to send: null appInstance");
            return false;
        }
        StringBuffer recipient = new StringBuffer();
        boolean toALL = appInstance.equalsIgnoreCase(INSTANCE_ALL);
        boolean toANY = appInstance.equalsIgnoreCase(INSTANCE_ANY);
        if (toALL || toANY) {
            recipient.append('/').append(appName).append('/');
        } else {
            recipient.append(appName).append('/').append(appInstance).append('/');
        }
        try {
            final String to = recipient.toString();
            if (_logger.isDebugEnabled()) {
                _logger.debug("Sending muxGlobalEvent to: " + to + ((toALL) ? " ALL" : "") + ((toANY) ? " ANY" : ""));
            }
            // formerly: Reporter.sendApplicationMessage (to, identifierI, toALL, identifierS, new
            // ByteArrayWrapper (data, off, len));

            ms.sendCommand(to, identifierI, toALL, identifierS, data, off, len);
        } catch (Throwable t) {
            _logger.warn("Exception while sending muxGlobalEvent", t);
            return false;
        }
        return true;
    }

    /**
     * Sends an event that will be sent to the specified appName in the current JVM.
     */
    @Override
    public boolean sendLocalEvent(String appName, String appInstance, int identifierI, String identifierS, Object data,
        boolean asynchronous)
    {
        if (asynchronous) {
            AsynchronousEventScheduler.schedule(new AsynchronousEvent(this,
                new MuxAsynchronousEvent(appName, appInstance, identifierS, data), identifierI));
            return true;
        }

        if (appInstance == null) {
            _logger.warn("Invalid muxLocalEvent to send: null appInstance");
            return false;
        }

        boolean toALL = appInstance.equalsIgnoreCase(INSTANCE_ALL);
        boolean toANY = appInstance.equalsIgnoreCase(INSTANCE_ANY);

        if (toALL || toANY) {
            if (appName == null) {
                _logger.warn("Invalid muxLocalEvent to send: null appName");
                return false;
            }
            MuxMonitorable[] list = null;
            synchronized (_monitorables) {
                list = (MuxMonitorable[]) _monitorables.get(appName);
            }
            if (list == null) {
                return false;
            }

            for (int i = 0; i < list.length; i++) {
                try {
                    list[i].muxLocalEvent(identifierI, identifierS, data);
                } catch (Throwable t) {
                    _logger.warn("Exception in MuxHandler while calling muxLocalEvent", t);
                }
                if (toANY)
                    break;
            }

            return true;
        }

        MuxHandler muxHandler = (MuxHandler) _monitorables.get("I:" + appInstance);
        if (muxHandler == null) {
            return false;
        }
        try {
            muxHandler.muxLocalEvent(identifierI, identifierS, data);
        } catch (Throwable t) {
            _logger.warn("Exception in MuxHandler while calling muxLocalEvent", t);
        }
        return true;
    }

    private static class MuxAsynchronousEvent {
        String _appName, _appInstance, _identifierS;
        Object _data;

        private MuxAsynchronousEvent(String appName, String appInstance, String identifierS, Object data) {
            this._appName = appName;
            this._appInstance = appInstance;
            this._identifierS = identifierS;
            this._data = data;
        }
    }

    /**************************************
     * Implementation of AsynchronousEventListener
     **************************************/

    @Override
    public void asynchronousEvent(Object data, int identifierI) {
        MuxAsynchronousEvent event = (MuxAsynchronousEvent) data;
        sendLocalEvent(event._appName, event._appInstance, identifierI, event._identifierS, event._data, false);
    }
}
