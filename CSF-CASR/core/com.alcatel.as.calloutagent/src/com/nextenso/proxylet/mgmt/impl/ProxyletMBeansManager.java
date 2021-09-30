package com.nextenso.proxylet.mgmt.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.modelmbean.ModelMBean;

import org.apache.log4j.Logger;

import com.alcatel.as.service.appmbeans.ApplicationMBeanFactory;
import com.nextenso.proxylet.engine.Context;
import com.nextenso.proxylet.engine.ProxyletChain;
import com.nextenso.proxylet.engine.ProxyletEnv;
import com.nextenso.proxylet.engine.ProxyletInvocationHandler;

import alcatel.tess.hometop.gateways.utils.Log;

public class ProxyletMBeansManager {
    
    /**
     * Attribute used to find the ModelMBean associated with a proxylet
     */
    private static final String ATTR_PREFIX_MODEL_MBEAN = "javax.management.modelmbean.";
    private static Log _log = Log.getLogger("callout.mbeans");
    private final static ProxyletMBeansManager _instance = new ProxyletMBeansManager();
    private final Map<Object, ModelMBean> _pxletMBeans = new HashMap();
    volatile ApplicationMBeanFactory _aBeanFactory;

    public static ProxyletMBeansManager getInstance() {
        return _instance;
    }
    
    public void setProxyletMBeansManager(ApplicationMBeanFactory appMbeanFactory) {
        _aBeanFactory = appMbeanFactory;
    }

    /**
     * Register the different proxylet MBeans of the Context on our MBean server.
     */
    public synchronized void registerProxyletMBeans(Context context, String scope) {
        ProxyletChain[] chains = context.getProxyletChains();
        _log.debug("registering proxylet mbeans for context %s ", context.getName());
        if (chains == null) {
            _log.debug("Ignoring proxylet context " + context.getName() + ": no chains found in context.");
            return;
        }

        String protocol = getProtocol(context.getMuxHandler().getInstanceName());

        for (int chainId = 0; chainId < chains.length; chainId++) {
            ProxyletChain chain = chains[chainId];
            ProxyletEnv[] proxyletEnvs = chain.getProxyletEnvs();

            if (proxyletEnvs == null) {
                _log.debug("Ignoring proxylet context " + context.getName() + ": no proxylet env found");
                continue;
            }

            for (int proxyletId = 0; proxyletId < proxyletEnvs.length; proxyletId++) {
                ProxyletEnv proxyletEnv = proxyletEnvs[proxyletId];
                registerProxyletMBean(proxyletEnv, protocol, scope);
            }
        }
    }

    /**
     * Unregister the different proxylet MBeans of the Context on our MBean server.
     */
    public synchronized void unregisterProxyletMBeans(Context context) {
        ProxyletChain[] chains = context.getProxyletChains();

        if (chains == null) {
            _log.debug("Ignoring proxylet context " + context.getName() + ": no chains found in context.");
            return;
        }

        for (int chainId = 0; chainId < chains.length; chainId++) {
            ProxyletChain chain = chains[chainId];
            ProxyletEnv[] proxyletEnvs = chain.getProxyletEnvs();

            if (proxyletEnvs == null) {
                _log.debug("Ignoring proxylet context " + context.getName() + ": no proxylet env found");
                continue;
            }

            String protocol = getProtocol(context.getMuxHandler().getInstanceName());
            ApplicationMBeanFactory aBeanFactory = _aBeanFactory;
            if (aBeanFactory != null) {
                for (int proxyletId = 0; proxyletId < proxyletEnvs.length; proxyletId++) {
                    ProxyletEnv proxyletEnv = proxyletEnvs[proxyletId];
                    try {
                        aBeanFactory.unregisterObject(proxyletEnv.getProxyAppEnv().getAppId(), protocol,
                            proxyletEnv.getProxyletName(), proxyletEnv.getProxyletContext().getMajorVersion(),
                            proxyletEnv.getProxyletContext().getMinorVersion());
                    } catch (InstanceNotFoundException e) {
                        if (_log.isDebugEnabled()) {
                            _log.debug("No registered mbean for " + proxyletEnv.getProxyletName());
                        }
                    } catch (JMException e) {
                        _log.warn("could not unregister proxylet mbean " + proxyletEnv.getProxyletName(), e);
                    }
                }
            }
        }
    }

    /**
     * Check if a MBean is responsible for the management of the given Proxylet. If a MBean is
     * found, it is registered on the local MBeanServer.
     */
    private void registerProxyletMBean(ProxyletEnv proxyletEnv, String protocol, String scope) {
        String pxletName = proxyletEnv.getProxyletName();
        String appId = proxyletEnv.getProxyAppEnv().getAppId();
        if (scope != null) {
            appId = appId + "." + scope;
        }
        ApplicationMBeanFactory aBeanFactory = _aBeanFactory;

        if (aBeanFactory == null) {
            _log.debug("Not registering proxylet mbeans (no mbean factory available)");
            return;
        }

        _log.debug(
            "Checking mbeans for proxylet " + pxletName + " from application: " + appId + "(scope=" + scope + ")");

        try {
            if (aBeanFactory.loadDescriptors(appId)) {
                int tries = 2;
                while (tries > 0) {
                    tries--;
                    // create the mbean
                    // proxylet instance points to a proxy, which points to the actual proxylet. Because
                    // reflection does not work on dynamic
                    // proxies, we have to point to the actual proxylet, and we'll activate the context
                    // class loader manually, without
                    // using the dynamic proxy ...
                    try {
                        _log.debug("Checking if mbean for pxlet " + pxletName + " is already registered  (current: "
                            + _pxletMBeans + ")");
                        ModelMBean mBean = null;
                        Object pxlet = ProxyletInvocationHandler.getProxylet(proxyletEnv.getProxylet());

                        if (!_pxletMBeans.containsKey(pxlet)) {
                            _log.debug("Creating mbean for pxlet " + pxlet);

                            mBean = aBeanFactory.registerObject(appId, protocol, pxlet, pxletName,
                                proxyletEnv.getProxyletContext().getMajorVersion(),
                                proxyletEnv.getProxyletContext().getMinorVersion());
                            _pxletMBeans.put(pxlet, mBean /* may be null */);
                        } else {
                            mBean = _pxletMBeans.get(pxlet);
                        }

                        if (mBean != null) {
                            // Set the mbean as an attribute of the context
                            _log.debug("Registering mbean into " + pxletName + " proxylet context" + ":" + mBean);
                            proxyletEnv.getContext().setAttribute(ATTR_PREFIX_MODEL_MBEAN + pxletName, mBean);
                        } else {
                            _log.info("Did not find any mbeans from pxlet " + proxyletEnv.getProxyletName());
                        }
                        tries = 0;
                    } catch (InstanceAlreadyExistsException e) {
                        _log.warn("destroying already registered mbean for " + pxletName, e);
                        aBeanFactory.unregisterObject(appId, protocol, pxletName,
                            proxyletEnv.getProxyletContext().getMajorVersion(),
                            proxyletEnv.getProxyletContext().getMinorVersion());
                    }
                }
                aBeanFactory.unloadDescriptors(appId);
            }
        } catch (IOException e) {
            _log.error("Could not load descriptors for " + appId, e);
        } catch (JMException e) {
            _log.error("Could not create mbean for " + pxletName, e);
        }
    }

    private synchronized String getProtocol(String instanceName) {
        if (instanceName != null) {
            return instanceName.substring(instanceName.lastIndexOf('-') + 1);
        }
        return "";
    }
}
