package com.nextenso.proxylet.engine.xml;

// XML
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public abstract class XMLUtils implements ErrorHandler, EntityResolver {
  
  //
  // Context tags
  //
  public static final String CONTEXT = "context";
  public static final String CONTEXT_ID = "id";
  public static final String CONTEXT_NAME = "context-name";
  public static final String DESCRIPTION = "description";
  public static final String CONTEXT_PARAM = "context-param";
  public static final String INIT_PARAM = "init-param";
  public static final String PARAM_NAME = "param-name";
  public static final String PARAM_VALUE = "param-value";
  public static final String CONTEXT_CRITERION = "context-criterion";
  public static final String PROXYLET_NAME = "proxylet-name";
  public static final String PROXYLET_CLASS = "proxylet-class";
  public static final String CONTEXT_LISTENER = "context-listener";
  public static final String LISTENER_CLASS = "listener-class";
  public static final String LISTENER_NAME = "listener-name";
  
  //
  // criterion tags
  //
  public static final String CRITERION = "criterion";
  public static final String CRITERION_NAME = "criterion-name";
  public static final String CRITERION_VALUE = "criterion-value";
  public static final String CRITERION_AND = "and";
  public static final String CRITERION_OR = "or";
  public static final String CRITERION_NOT = "not";
  public static final String CRITERION_REF = "criterion-ref";
  public static final String CRITERION_ALL = "all";
  public static final String CRITERION_FROM = "from";
  public static final String CRITERION_UNTIL = "until";
  public static final String CRITERION_DAY = "day";
  public static final String CRITERION_DATE = "date";
  public static final String CRITERION_MONTH = "month";
  public static final String CRITERION_TIME = "time";
  public static final String CRITERION_ATTR_NAME = "attr-name";
  public static final String CRITERION_ATTR_VALUE = "attr-value";
  
  private static DocumentBuilderFactory documentBuilderFactory;
  static {
    documentBuilderFactory = DocumentBuilderFactory.newInstance();
    documentBuilderFactory.setValidating(true);
  }
  
  protected DocumentBuilder getDocumentBuilder() {
    try {
      return documentBuilderFactory.newDocumentBuilder();
    } catch (Throwable e) {
      // should never happen
      throw new RuntimeException("Exception while loading an XML Document builder: " + e);
    }
  }
  
  protected static String getString(Element element, boolean mandatory, String errorText)
      throws XMLConfigException {
    String s = null;
    boolean invalid = false;
    Node node = element.getFirstChild();
    if (node != null) {
      s = node.getNodeValue();
      if (s != null) {
        s = s.trim();
        invalid = (s.length() == 0);
      } else
        invalid = true;
    } else
      invalid = true;
    if (mandatory && invalid)
      throw new XMLConfigException(errorText);
    return s;
  }
  
  protected static ArrayList getChildElements(Element e) {
    ArrayList children = new ArrayList();
    NodeList list = e.getChildNodes();
    int l = list.getLength();
    for (int i = 0; i < l; i++) {
      Node n = list.item(i);
      if (n.getNodeType() != Node.ELEMENT_NODE)
        continue;
      children.add(n);
    }
    return children;
  }
  
  private Hashtable instances = new Hashtable();
  
  protected void registerInstance(String name, Object o) {
    instances.put(name, o);
  }
  
  protected Object getInstance(String name) {
    return instances.get(name);
  }
  
  protected void clearInstances() {
    instances.clear();
  }
  
  protected Object parseListener(Element element, Class listenerClass, String contextId)
      throws XMLConfigException {
    String name = null;
    Object listener = null;
    ArrayList sublist = getChildElements(element);
    for (int j = 0; j < sublist.size(); j++) {
      Element e = (Element) sublist.get(j);
      String t = e.getTagName();
      if (LISTENER_NAME.equals(t)) {
        name = getString(e, true, "Missing Listener name for context: " + contextId);
        continue;
      }
      if (LISTENER_CLASS.equals(t)) {
        if (name != null) {
          if (getInstance(name) != null)
            throw new XMLConfigException("Invalid listener name <" + name + "> for context: " + contextId);
        }
        String classname = getString(e, true, "Missing Listener class for context: " + contextId);
        try {
          listener = Class.forName(classname).newInstance();
          if (!listenerClass.isInstance(listener))
            throw new XMLConfigException("Invalid listener class for context: " + contextId);
        } catch (Throwable exc) {
          throw new XMLConfigException("Exception while loading class: " + classname + " : " + exc);
        }
        if (name != null)
          registerInstance(name, listener);
      }
    }
    if (listener == null) {
      Object o = getInstance(name);
      if (o == null)
        throw new XMLConfigException("Invalid listener name <" + name + "> for context: " + contextId);
      if (listenerClass.isInstance(o))
        listener = o;
      else
        throw new XMLConfigException("Invalid listener name for context: " + contextId);
    }
    return listener;
  }
  
  /**********************************
   * Implementation of ErrorHandler
   **********************************/
  
  public void error(SAXParseException e) throws SAXParseException {
    throw e;
  }
  
  public void fatalError(SAXParseException e) throws SAXParseException {
    throw e;
  }
  
  public void warning(SAXParseException e) throws SAXParseException {
    throw e;
  }
  
  /**********************************
   * Implementation of PropValInterface
   **********************************/
  
  @SuppressWarnings("unused")
  public String isValueValid(String module, String instance, boolean isPrivate, String name, String oldValue,
                             String newValue) {
    if (isPrivate && newValue.length() == 0)
      // ok - blanked out
      return null;
    DocumentBuilder parser = getDocumentBuilder();
    parser.setEntityResolver(this);
    parser.setErrorHandler(this);
    try {
      parser.parse(new InputSource(new StringReader(newValue)));
    } catch (SAXParseException spe) {
      return ("XML Exception: " + spe.getMessage() + " at line " + spe.getLineNumber());
    } catch (Throwable t) {
      return ("Exception: " + t);
    }
    return null;
  }
  
  /**********************************
   * Implementation of EntityResolver
   **********************************/
  
  public abstract InputSource resolveEntity(String publicId, String systemId) throws SAXException,
      java.io.IOException;
}
