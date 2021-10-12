// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.licensing.agnosticImpl.cljlPrefs;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;


/**
 * Node in cached preferences tree.
 */
public class CacheNode {
    /** Name */
    private String nodeName = null;

    private String absolutePath = null;

    /** Parent */
    private CacheNode nodeParent = null;

    /** Children */
    private ArrayList<CacheNode> nodeChildren = null;

    /** Keys and values */
    private LinkedHashMap<String, String> attributes = null;

    /**
     * Constructor
     *
     * @param name
     *            Node's name
     * @param parent
     *            Node's parent
     */
    public CacheNode(final String path, final String name, final CacheNode parent) {
        this.nodeChildren = new ArrayList<CacheNode>();
        this.attributes = new LinkedHashMap<String, String>();
        this.nodeName = name;
        this.absolutePath = path;
        this.nodeParent = parent;
    }

    /**
     * Adds an attribute to node.
     *
     * @param key
     *            key
     * @param value
     *            value
     */
    public void addAttribute(final String key, final String value) {
        this.attributes.put(key, value);
    }

    /**
     * Gets value by key.
     *
     * @param key
     *            key
     * @return the value to which this map maps the specified key, or null if the map contains no mapping for this key.
     */
    public String getValue(final String key) {
        return this.attributes.get(key);
    }

    /**
     * Gets keys.
     *
     * @return Keys as strings
     */
    public List<String> getKeys() {
        return new ArrayList<String>(this.attributes.keySet());
    }

    /**
     * Gets name.
     *
     * @return name
     */
    public String getName() {
        return this.nodeName;
    }

    /**
     * Gets name with full path.
     *
     * @return path
     */
    public String getAbsoluteName() {
        if (this.absolutePath != null) {
            return this.absolutePath;
        }

        final StringBuffer buf = new StringBuffer(256);
        buf.append("/");
        CacheNode node = this;
        while (node != null) {
            final String name = node.getName();
            if (name.length() > 0) {
                buf.insert(0, name);
                buf.insert(0, "/");
            }
            node = node.getParent();
        }

        final int len = buf.length();
        if (len > 1 && buf.substring(len - 1).equals("/")) {
            // Not root node and last char is SLASH -> remove it
            this.absolutePath = buf.substring(0, len - 1);
            return this.absolutePath;
        }
        this.absolutePath = buf.toString();
        return this.absolutePath;
    }

    /**
     * Gets parent
     *
     * @return parent
     */
    public CacheNode getParent() {
        return this.nodeParent;
    }

    /**
     * Adds a child
     *
     * @param child
     *            child
     */
    public void addChild(final CacheNode child) {
        if (child == null) {
            return;
        }
        this.nodeChildren.add(child);
    }

    /**
     * Returns child, null if it doesn't exist
     *
     * @param childName
     *            The child which is wanted
     * @return child
     */
    public CacheNode getChild(final String childName) {
        for (CacheNode node : this.nodeChildren) {
            if (node.getName().equals(childName)) {
                return node;
            }
        }
        return null;
    }

    /**
     * Gets children.
     *
     * @return children
     */
    public List<CacheNode> getChildren() {
        return this.nodeChildren;
    }
}
