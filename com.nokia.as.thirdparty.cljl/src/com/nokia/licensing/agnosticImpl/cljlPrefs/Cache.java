package com.nokia.licensing.agnosticImpl.cljlPrefs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.prefs.BackingStoreException;


public class Cache {
    /** System roots */
    private final Map<XmlSupport, CacheNode> systemRootNodes;

    /** User roots */
    private final Map<XmlSupport, CacheNode> userRootNodes;

    /** Constructor constructs Map variables */
    public Cache() {
        this.systemRootNodes = new HashMap<XmlSupport, CacheNode>();
        this.userRootNodes = new HashMap<XmlSupport, CacheNode>();
    }

    /**
     * Adds a plugin to cache
     *
     * @param plugin
     *            the plugin to add
     */
    public void addPluginData(final XmlSupport xml) {
        final CacheNode systemRootNode = new CacheNode("/", "", null);
        readNodeFromPlugin(systemRootNode, xml, true);
        this.systemRootNodes.put(xml, systemRootNode);

        final CacheNode userRootNode = new CacheNode("/", "", null);
        readNodeFromPlugin(userRootNode, xml, false);
        this.userRootNodes.put(xml, userRootNode);
    }

    /**
     * Returns keys from given path
     *
     * @param path
     *            The path
     * @param isSystemPref
     *            System or user preference
     * @return keys from given path
     */
    public List<String> getKeys(final String path, final boolean isSystemPref) {
        final List<String> keys = new ArrayList<String>();

        Set<XmlSupport> set = null;
        if (isSystemPref) {
            set = this.systemRootNodes.keySet();
        } else {
            set = this.userRootNodes.keySet();
        }
        for (final XmlSupport xmlSupport : set) {
            getKeys(path, isSystemPref, xmlSupport, keys);
        }
        return keys;
    }

    /**
     * Gets keys from given path and plugin.
     *
     * @param path
     *            The path where to get keys
     * @param isSystemPref
     *            system or user
     * @param plugin
     *            The plugin which cache is read
     * @param keys
     *            The list where keys are added
     */
    private void getKeys(final String path, final boolean isSystemPref, final XmlSupport xml, final List<String> keys) {
        if (path == null) {
            return;
        }

        final CacheNode node = getNode(path, getRootNode(isSystemPref, xml));
        if (node != null) {
            keys.addAll(node.getKeys());
        }
        return;

    }

    /**
     * Searches CacheNode from rootNode down. Returns found node with given path. If rootNode is null or searched node
     * is not found then null is returned
     *
     * @param path
     *            path to the node
     * @param rootNode
     *            CacheNode containing other child nodes
     * @return found node with given path. If rootNode is null or searched node is not found then null is returned
     */
    private CacheNode getNode(final String path, CacheNode rootNode) {

        final StringTokenizer token = new StringTokenizer(path, "/");
        if (rootNode == null) {
            return null;
        }
        // node = node.getChild(token.nextToken()); //skip root
        boolean moreElements = true;
        while (moreElements) {
            if (rootNode.getAbsoluteName().equals(path)) {
                return rootNode;
            }
            moreElements = token.hasMoreElements();
            if (moreElements) {
                final CacheNode node = rootNode.getChild(token.nextToken());
                if (node != null) {
                    rootNode = node;
                }
            }
        }
        return null;
    }

    /**
     * Return children from given path
     *
     * @param path
     *            The path where to get children
     * @param isSystemPref
     *            system or user preference
     * @return children
     */
    public List<CacheNode> getChildren(final String path, final boolean isSystemPref) {
        final List<CacheNode> children = new ArrayList<CacheNode>();

        Set<XmlSupport> set = null;
        if (isSystemPref) {
            set = this.systemRootNodes.keySet();
        } else {
            set = this.userRootNodes.keySet();
        }
        for (final XmlSupport xmlSupport : set) {
            getChildren(path, isSystemPref, xmlSupport, children);
        }
        return children;
    }

    /**
     * Gets children from given path and plugin.
     *
     * @param path
     *            The path where to get children
     * @param isSystemPref
     *            system or user
     * @param plugin
     *            The plugin which cache is read
     * @param children
     *            The list where children are added
     */
    private void getChildren(final String path, final boolean isSystemPref, final XmlSupport xml,
            final List<CacheNode> children) {
        if (path == null) {
            return;
        }
        final String[] parts = path.split("/");

        CacheNode node = getRootNode(isSystemPref, xml);

        for (final String part : parts) {
            if (part == null || part.length() == 0 || node == null) {
                continue;
            }
            node = node.getChild(part);
        }

        if (node != null && node.getAbsoluteName().equals(path)) {
            children.addAll(node.getChildren());
        }
    }

    /**
     * Returns value from given path and key
     *
     * @param plugin
     *            Plugin which cache should be searched
     * @param path
     *            path
     * @param key
     *            key
     * @param isSystemPref
     *            system or user preference
     * @return the value, null if not found
     */
    public String getValue(final XmlSupport xml, final String path, final String key, final boolean isSystemPref) {
        if (xml == null || path == null) {
            return null;
        }

        final CacheNode node = getNode(path, getRootNode(isSystemPref, xml));
        if (node != null) {
            return node.getValue(key);
        } else {
            return null;
        }
    }

    /**
     * Returns user or system root of the requested plugin.
     *
     * @param isSystemPref
     *            system or user roots
     * @param plugin
     *            The plugin
     * @return roots
     */
    private CacheNode getRootNode(final boolean isSystemPref, final XmlSupport xml) {
        if (isSystemPref) {
            return this.systemRootNodes.get(xml);
        }
        return this.userRootNodes.get(xml);
    }

    /**
     * Reads a node from plugin data for caching it.
     *
     * @param node
     *            node
     * @param plugin
     *            plugin
     * @param isSystemPref
     *            isSystemPref
     */
    private void readNodeFromPlugin(final CacheNode node, final XmlSupport xml, final boolean isSystemPref) {
        try {
            final String nodeName = node.getAbsoluteName();

            final List<String> keys = xml.getKeys(isSystemPref, nodeName);
            for (String key : keys) {
                final String value = xml.getValue(isSystemPref, nodeName, key);
                node.addAttribute(key, value);
            }

            final List<String> children = xml.getChildrenNames(isSystemPref, nodeName);
            for (String childName : children) {
                // Skip invalid node names... otherwise we might end up in an infinite recursion.
                if (isValidNodeName(childName)) {
                    final StringBuffer buf = new StringBuffer(256);
                    buf.append(nodeName);
                    if (!nodeName.equals("/")) {
                        buf.append("/");
                    }
                    buf.append(childName);
                    final CacheNode child = new CacheNode(buf.toString(), childName, node);

                    node.addChild(child);
                    readNodeFromPlugin(child, xml, isSystemPref);
                } else {
                    System.out.println("skipping child as it is empty");
                }
            }
        } catch (final BackingStoreException e) {
            // TODO: nothing?
        }
    }

    /**
     * Checks whether a node name is valid (when listing children).
     *
     * @param name
     *            Node name
     * @return true if valid, false if not.
     */
    private boolean isValidNodeName(final String name) {
        // Don't include as a child if child's name is empty... that will break the absolute node path evaluation.
        return name != null && name.length() > 0;
    }
}
