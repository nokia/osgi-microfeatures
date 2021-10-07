package com.nokia.licensing.agnosticImpl.cljlPrefs;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXParseException;


/**
 * This class is responsible for validating preferences file. The configuration file has a strict structure with:
 * <ul>
 * <li><b>preferences</b> - root element, with optional DTD version attribute: <b>EXTERNAL_XML_VERSION</b></li>
 * <li><b>root</b> - main element, with mandatory attribute: type with two possible values <b>system</b> or <b>user</b>)
 * </li>
 * <li><b>map</b> - element, which collects root preferences, if any</li>
 * <li><b>entry</b> - optional element in the root map, which has two mandatory attributes: <b>key</b> and <b>value</b>
 * </li>
 * <li><b>node</b> - element, which defines a map of possible entries and possible child node</li>
 * </ul>
 *
 * Example preferences file could look like:
 *
 * <pre>
 <preferences EXTERNAL_XML_VERSION="0.0">
    <root type="system">
        <map>
            <entry key="Text" value="Text"/>
        </map>
        <node name="Text">
            <map>
                <entry key="Text" value="Text"/>
            </map>
        </node>
    </root>
</preferences>
 * </pre>
 *
 * @author Jakub Rudzki / Solita Oy <br>
 * @date Nov 6, 2006
 */
public class PreferencesValidator {
    private static final boolean OPTIONAL = false;
    private static final boolean REQUIRED = true;
    private static final boolean NOT_FOUND = false;
    private static final boolean FOUND = true;

    /**
     * Validates a preferences document.
     *
     * @param doc
     *            The document to be validated
     * @throws SAXParseException
     *             If validation fails
     */
    static public void validateDocument(final Document doc) throws SAXParseException {
        // Expect preferences
        final Node preferences = getChild(doc);
        compare(preferences, "preferences", REQUIRED);
        checkAttributes(preferences, "EXTERNAL_XML_VERSION", null, // No expected values
                OPTIONAL);

        // Expect root
        final Node root = getChild(preferences);
        compare(root, "root", REQUIRED);
        checkAttributes(root, "type", new String[] {
                "system", "user" }, REQUIRED);

        // Expect map
        final Node map = getChild(root);
        validateMap(map);

        // Expect optional node
        Node node = getSibling(map);
        while (node != null) {
            validateNode(node);
            node = getSibling(node);
        }
    }

    /**
     * Validates one node element, which should contain at least one map and possibly other node elements.
     *
     * @param node
     *            The node to be validated
     * @throws SAXParseException
     *             If the validation fails
     */
    private static void validateNode(final Node node) throws SAXParseException {
        compare(node, "node", REQUIRED);

        final Node map = getChild(node);
        validateMap(map);

        // Check for possible other nodes
        Node otherNode = getSibling(map);
        while (otherNode != null) {
            validateNode(otherNode);

            otherNode = getSibling(otherNode);
        }
    }

    /**
     * Validates a map element, which can contain optional enrty elemens
     *
     * @param map
     *            The map to be validated
     * @throws SAXParseException
     *             If the validation fails
     */
    private static void validateMap(final Node map) throws SAXParseException {
        if (map == null) {
            throw new SAXParseException("Mandatory 'map' element not found!", null);
        }
        compare(map, "map", REQUIRED);

        Node entry = getChild(map);
        // Entries are optional in a map
        while (entry != null) {
            // Check that it is an entry
            compare(entry, "entry", REQUIRED);

            // Check required 'key' attribute
            checkAttributes(entry, "key", null, REQUIRED);

            // Check required 'value' attribute
            checkAttributes(entry, "value", null, REQUIRED);

            // Get another entry
            entry = getSibling(entry);
        }
    }

    /**
     * Returns any child Element of the document node
     *
     * @param node
     *            The node which child should be returned
     * @return The child element, or null if the node does not have any Element child
     */
    private static Node getChild(final Node node) {
        Node child = node.getFirstChild();
        while (child != null) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                return child;
            }
            child = child.getNextSibling();
        }
        return child;
    }

    /**
     * Returns a sibling Element of the document node
     *
     * @param node
     *            The node which sibling should be returned
     * @return The sibling element, or null if the node does not have any Element sibling
     */
    private static Node getSibling(final Node node) {
        Node sibling = node.getNextSibling();
        while (sibling != null) {
            if (sibling.getNodeType() == Node.ELEMENT_NODE) {
                return sibling;
            }
            sibling = sibling.getNextSibling();
        }
        return sibling;
    }

    /**
     * Compares a node's nome and expected name.
     *
     * @param node
     *            The node which name should be compared
     * @param expectedName
     *            The expected node name
     * @param required
     *            If true and the name do not match the expected one, an exception is thrown
     * @return True if the names match, false if the names do not match and the name is not required
     * @throws SAXParseException
     *             If the names do not match and the name is required
     */
    private static boolean compare(final Node node, final String expectedName, final boolean required)
            throws SAXParseException {
        if (node == null && required) {
            throw new SAXParseException("Incorrect node! Expected: '" + expectedName + "', but was 'null'", null);
        }
        if (node == null && !required) {
            return NOT_FOUND;
        }
        final String name = node.getNodeName();
        if (!expectedName.equals(name)) {
            if (required) {
                throw new SAXParseException("Incorrect node! Expected: '" + expectedName + "', but was '" + name + "'",
                        null);
            }
            return NOT_FOUND;
        }
        return FOUND;
    }

    /**
     * Checks attributed of the given node. Is is first checked whether the expected attribute exists. If it does and
     * there are provided possible attribute values, the values are checked.
     *
     * @param node
     *            The node to be checked for the attribute
     * @param attributeName
     *            The name of the attribure expected in the node
     * @param values
     *            Expected values of the atribute, or null if the values do not matter
     * @param required
     *            If true and the attribute does not exist, an exception is thrown
     * @return True if the attribute is found (and containd expected values), false if the attribute (or its values) is
     *         not found and the attribute is not required
     * @throws SAXParseException
     *             If the attribute is required and not found
     */
    private static boolean checkAttributes(final Node node, final String attributeName, final String[] values,
            final boolean required) throws SAXParseException {
        final NamedNodeMap attributes = node.getAttributes();
        if (attributes != null) {
            final Node attr = attributes.getNamedItem(attributeName);
            if (compare(attr, attributeName, required)) {
                final String value = attr.getNodeValue();
                if (values != null) {
                    for (final String value2 : values) {
                        if (value2.equals(value)) {
                            return FOUND;
                        }
                    }
                } else {
                    if (value != null) {
                        return FOUND;
                    }
                }
            }
            return NOT_FOUND;
        } else {
            if (required) {
                throw new SAXParseException(
                        "Incorrect node! " + "Expected attribute '" + attributeName + "' not found.", null);
            }
            return NOT_FOUND;
        }
    }
}
