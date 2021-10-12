// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.licensing.agnosticImpl.cljlPrefs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.prefs.BackingStoreException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;


/**
 * Plugin implementation for reading XML files.
 */
public class XmlSupport {
    /**
     * List of system root nodes. Each xml file can contain either system or user root
     */
    private Node systemRoot = null;

    /**
     * List of user root nodes. Each xml file can contain either system or user root
     */
    private Node userRoot = null;

    /**
     * The XML Validation Error Handler responsible for registering any parsing errors in the file
     */
    private final XmlValidationErrorHandler errorHandler = new XmlValidationErrorHandler();

    public void initialize(final File file) {
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setValidating(true);
        try {
            final DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
            builder.setErrorHandler(this.errorHandler);

            final FileInputStream is = new FileInputStream(file);
            this.errorHandler.setFileName(file.getName());
            builder.setEntityResolver(new DTDResolver());
            final Document doc = builder.parse(is, "");
            validate(doc);
            is.close();
            final Node docRoot = doc.getDocumentElement();
            // 'root' elements in 'preferences' root node
            final NodeList roots = docRoot.getChildNodes();
            for (int r = 0; r < roots.getLength(); r++) {
                final Node rootNode = roots.item(r);
                if (rootNode.getNodeType() == Node.ELEMENT_NODE) {
                    final NamedNodeMap attrs = rootNode.getAttributes();
                    final Node n = attrs.getNamedItem("type");
                    if (n != null && n.getNodeType() == Node.ATTRIBUTE_NODE) {
                        final String value = n.getNodeValue();
                        if ("system".equals(value)) {
                            this.systemRoot = rootNode;
                        } else if ("user".equals(value)) {
                            this.userRoot = rootNode;
                        }
                    }
                }
            }
        } catch (final SAXException e) {

        } catch (final ParserConfigurationException e) {

        } catch (final FileNotFoundException e) {

        } catch (final IOException e) {

        }
    }

    public String getValue(final boolean isSystemPref, final String path, final String key) {
        if (path == null || key == null) {
            return null;
        }
        final List<Node> entryNodes = getEntryNodes(isSystemPref, path, key);
        if (entryNodes.size() == 0) {
            return null;
        }
        final Node node = entryNodes.get(0);
        final NamedNodeMap attrs = node.getAttributes();
        final Node valueAttr = attrs.getNamedItem("value");
        if (valueAttr == null) {
            return null;
        }
        return valueAttr.getNodeValue();
    }

    /**
     * Returns all of the keys that have an associated value in the specified path's node. (The returned array will be
     * of size zero if this path has no preferences.) The exception will be thrown if this operation cannot be completed
     * due to a failure in the backing store, or inability to communicate with it.
     *
     * @param isSystemPref
     *            True if names should be got from system preferences
     * @param path
     *            Path of the keys
     * @return keys
     * @throws BackingStoreException
     *             if storage is not available
     * @see com.nokia.oss.cseprefs.plugins.PreferencesPlugin#getKeys(boolean, java.lang.String)
     */
    public List<String> getKeys(final boolean isSystemPref, final String path) throws BackingStoreException {
        if (path == null) {
            return new ArrayList<String>();
        }

        final List<Node> entryNodes = getEntryNodes(isSystemPref, path, null);
        final List<String> keys = new ArrayList<String>();
        Node node = null;
        Node keyAttr = null;

        for (final Iterator<Node> it = entryNodes.iterator(); it.hasNext();) {
            node = it.next();
            keyAttr = node.getAttributes().getNamedItem("key");
            keys.add(keyAttr.getNodeValue());
        }
        return keys;
    }

    /**
     * Returns the names of the children of the specified path's node. (The returned array will be of size zero if this
     * node has no children.) The exception will be thrown if this operation cannot be completed due to a failure in the
     * backing store, or inability to communicate with it.
     *
     * @param isSystemPref
     *            True if names should be got from system preferences
     * @param path
     *            Path of the node which children are returned
     * @throws BackingStoreException
     *             if storage is not available
     * @return children names
     * @see com.nokia.oss.cseprefs.plugins.PreferencesPlugin#getChildrenNames(boolean, java.lang.String)
     */
    public List<String> getChildrenNames(final boolean isSystemPref, final String path) throws BackingStoreException {
        if (path == null) {
            return new ArrayList<String>();
        }
        final Node rootNode = getRoot(isSystemPref);

        Node root = null;
        Node node = null;
        Node attr = null;
        Node currentNode = null;
        NodeList children = null;
        NamedNodeMap attrs = null;

        final ArrayList<String> keyList = new ArrayList<String>();

        root = rootNode;
        node = getNode(root, path);
        if (node != null) {
            children = node.getChildNodes();
            // Get children that node name is "node" and has attribute
            // "name"
            for (int i = 0; i < children.getLength(); i++) {
                currentNode = children.item(i);
                if (currentNode.getNodeType() == Node.ELEMENT_NODE && "node".equals(currentNode.getNodeName())) {
                    attrs = currentNode.getAttributes();
                    if (attrs != null) {
                        attr = attrs.getNamedItem("name");
                        if (attr != null) {
                            final String nodeValue = attr.getNodeValue();
                            // Don't include as a child if child's name
                            // is empty... that will break the absolute
                            // node path evaluation.
                            if (isValidNodeName(nodeValue)) {
                                keyList.add(nodeValue);
                            } else {
                                System.out.println("skip child which is empty");
                            }
                        }
                    }
                }
            }
        }
        return keyList;
    }

    private boolean isValidNodeName(final String name) {
        // Don't include as a child if child's name
        // is empty... that will break the absolute
        // node path evaluation.
        return name != null && name.length() > 0;
    }

    private List<Node> getEntryNodes(final boolean isSystemPref, final String path, final String key) {
        final Node rootNode = getRoot(isSystemPref);

        Node root = null;
        Node node = null;
        Node keyAttr = null;
        NodeList children = null;
        NodeList mapChildren = null;
        NamedNodeMap attrs = null;

        final ArrayList<Node> keyList = new ArrayList<Node>();

        root = rootNode;
        node = getNode(root, path); // i.e. AP/Java_Logging node
        if (node == null) {
            return keyList;
        }
        children = node.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            node = children.item(index);
            if (node.getNodeType() != Node.ELEMENT_NODE || (!"map".equals(node.getNodeName()))) {
                continue;
            }
            // Get children from <map> node
            mapChildren = node.getChildNodes();
            for (int mapChildIndex = 0; mapChildIndex < mapChildren.getLength(); mapChildIndex++) {
                node = mapChildren.item(mapChildIndex);
                if (node.getNodeType() != Node.ELEMENT_NODE || (!"entry".equals(node.getNodeName()))) {
                    continue;
                }
                attrs = node.getAttributes();
                keyAttr = attrs.getNamedItem("key");
                if (keyAttr == null) {
                    continue;
                }
                final String keyName = keyAttr.getNodeValue();
                if (key != null) {
                    if (keyName.equals(key)) {
                        keyList.add(node);
                        return keyList;
                    }
                } else {
                    keyList.add(node);
                }
            }
        }
        return keyList;
    }

    /**
     * Gets a node for a path from given root.
     *
     * @param root
     *            System root or user root
     * @param path
     *            path
     * @return A node
     */
    private Node getNode(final Node root, String path) {
        if (path.startsWith("/")) {
            path = path.replaceFirst("/", "");
        }
        final String[] parts = path.split("/");
        Node currentNode = root;
        if (path.length() == 0) {
            return currentNode;
        }
        boolean found = false;

        for (final String part : parts) {
            found = false;
            // Search a sibling node which is ELEMENT_NODE and it's name is
            // 'node'
            final NodeList children = currentNode.getChildNodes();
            for (int index = 0; index < children.getLength(); index++) {
                currentNode = children.item(index);
                if (currentNode.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                if (!"node".equals(currentNode.getNodeName())) {
                    continue;
                }
                final NamedNodeMap attrs = currentNode.getAttributes();
                if (attrs == null) {
                    continue;
                }
                final Node nameAttr = attrs.getNamedItem("name");
                if (nameAttr == null) {
                    continue;
                }

                if (part.equals(nameAttr.getNodeValue())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return null;
            }
        }
        return currentNode;
    }

    /**
     * Returns list of system roots or user roots
     *
     * @param isSystemPref
     *            True if system pref
     * @return The list
     */
    private Node getRoot(final boolean isSystemPref) {
        if (isSystemPref) {
            return this.systemRoot;
        }
        return this.userRoot;
    }

    /**
     * Validates preferences file. The validation is done manually, but according to the definition of the preference
     * file structure.
     *
     * @param doc
     *            The document to be validated
     * @throws SAXException
     *             If the document is invalid
     *
     */
    private void validate(final Document doc) throws SAXException {
        try {
            PreferencesValidator.validateDocument(doc);
        } catch (final SAXParseException e) {
            this.errorHandler.error(e);
        }
    }
}
