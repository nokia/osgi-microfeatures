package com.nokia.licensing.agnosticImpl.cljlPrefs;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;


public class PreferencesImpl extends AbstractPreferences {

    private Cache cache = null;
    private XmlSupport xml = null;
    boolean isUserNode = false;

    protected PreferencesImpl() {
        super(null, "");
        this.cache = new Cache();
        final File file = new File("/opt/nokia/oss/conf/Pref_system_LicenseInstall.xml");
        this.xml = new XmlSupport();
        this.xml.initialize(file);
        this.cache.addPluginData(this.xml);
    }

    public PreferencesImpl(final AbstractPreferences parent, final String name, final Cache newCache,
            final XmlSupport newXml, final boolean localIsUserNode) {

        super(parent, name);
        this.cache = newCache;
        this.xml = newXml;
        this.isUserNode = localIsUserNode;
    }

    public PreferencesImpl createUserPreferencesImpl() {
        final boolean localIsUserNode = true;
        return new PreferencesImpl(null, "", this.cache, this.xml, localIsUserNode);
    }

    @Override
    public String[] keysSpi() throws BackingStoreException {
        final List<String> list = new ArrayList<String>();
        list.addAll(this.cache.getKeys(absolutePath(), true));
        // Convert list to array
        final String[] keys = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            keys[i] = list.get(i);
        }
        return keys;
    }

    @Override
    public String getSpi(final String key) {
        final String value = this.cache.getValue(this.xml, absolutePath(), key, true);
        if (value != null) {
            return value;
        } else {
            return null;
        }
    }

    @Override
    public String[] childrenNamesSpi() throws BackingStoreException {
        final List<String> list = new ArrayList<String>();
        final List<CacheNode> children = this.cache.getChildren(absolutePath(), true);
        CacheNode node = null;
        for (final Iterator<CacheNode> it = children.iterator(); it.hasNext();) {
            node = it.next();
            list.add(node.getName());
        }
        // Convert list to array
        final String[] names = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            names[i] = list.get(i);
        }
        return names;
    }

    @Override
    public AbstractPreferences childSpi(final String name) {
        return new PreferencesImpl(this, name, this.cache, this.xml, this.isUserNode);
    }

    @Override
    public void putSpi(final String key, final String value) {
        throw new RuntimeException("Method not supported");
    }

    @Override
    public void flushSpi() throws BackingStoreException {
        throw new RuntimeException("Method not supported");
    }

    @Override
    public void removeNodeSpi() throws BackingStoreException {
        throw new RuntimeException("Method not supported");
    }

    @Override
    public void sync() throws BackingStoreException {
        syncSpi();
    }

    @Override
    public void syncSpi() throws BackingStoreException {
        throw new RuntimeException("Method not supported");
    }

    @Override
    public void removeSpi(final String key) {
        throw new RuntimeException("Method not supported");
    }
}
