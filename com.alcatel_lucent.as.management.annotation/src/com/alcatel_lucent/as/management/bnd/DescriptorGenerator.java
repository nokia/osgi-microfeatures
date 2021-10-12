// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.management.bnd;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collection;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.EmbeddedResource;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.bnd.osgi.Clazz.QUERY;

/**
 * This helper parses all classes which contain MBD annotations, and generates the corresponding mbeans descriptors.
 */
public class DescriptorGenerator extends Processor {
  /**
   * This is the bnd analyzer used to lookup classes containing DM annotations.
   */
  private Analyzer m_analyzer;
  
  /**
   * This is the generated Dependency Manager descriptors. The hashtable key is the path
   * to a descriptor. The value is a bnd Resource object which contains the content of a
   * descriptor. 
   */
  Map<String, Resource> m_resources = new HashMap<String, Resource>();
  
  /**
   * Creates a new descriptor generator.
   * @param analyzer The bnd analyzer used to lookup classes containing DM annotations.
   */
  public DescriptorGenerator(Analyzer analyzer) {
    super(analyzer);
    m_analyzer = analyzer;
  }
  
  /**
   * Starts the scanning.
   * @param _generateMonconf 
   * @return true if some annotations were successfully parsed, false if not. corresponding generated 
   * descriptors can then be retrieved by invoking the getDescriptors/getDescriptorPaths methods.
   */
  public boolean execute() throws Exception {
    boolean annotationsFound = false;
    Clazz clazz = null;
    try {
      // Try to locate any classes in the wildcarded universe
      // that are annotated with the MBD annotations.
      
      MonconfPropertiesBuilder monconfPropBuilder = new MonconfPropertiesBuilder();
      
      Collection<Clazz> expanded = m_analyzer.getClasses("", QUERY.NAMED.toString(), "*");
      for (Clazz c : expanded) {
        clazz = c;
        
        // Let's parse all annotations from that class !
        AnnotationCollector reader = new AnnotationCollector(this);
        c.parseClassFileWithCollector(reader);
        if (reader.finish() && reader.isPrintable()) {
          // And store the generated component descriptors in our resource list.
          String name = c.getFQN();
          Resource resource = createComponentResource(reader);
          m_resources.put("META-INF/" + name + ".mbd", resource);
          annotationsFound = true;
          reader.generateMonconf(monconfPropBuilder);
        }
      }
      
      monconfPropBuilder.write();
      return annotationsFound;
    }
    
    catch (Throwable err) {
      // We need to throw an Error in order to avoid bnd generating the target bundle ...
      StringBuilder sb = new StringBuilder();
      sb.append("Exception while scanning annotations");
      if (clazz != null) {
        sb.append(" from class " + clazz);
      }
      sb.append(System.getProperty("line.separator"));
      sb.append(parse(err));
      m_analyzer.error(sb.toString());
      return false;
    }
    
    finally {
      // Collect all logs (warns/errors) from our processor and store them into the analyze.
      // Bnd will log them, if necessary.
      m_analyzer.getInfo(this, "MBDAnnotation Parser: ");
      close();
    }
  }
  
  /**
   * Returns the path of the descriptor.
   * @return the path of the generated descriptors.
   */
  public String getDescriptorPaths() {
    StringBuilder descriptorPaths = new StringBuilder();
    String del = "";
    for (Map.Entry<String, Resource> entry : m_resources.entrySet()) {
      descriptorPaths.append(del);
      descriptorPaths.append(entry.getKey());
      del = ",";
    }
    return descriptorPaths.toString();
  }
  
  /**
   * Returns the list of the generated descriptors.
   * @return the list of the generated descriptors.
   */
  public Map<String, Resource> getDescriptors() {
    return m_resources;
  }
  
  /**
   * Creates a bnd resource that contains the generated dm descriptor.
   * @param collector 
   * @return
   * @throws IOException
   */
  private Resource createComponentResource(AnnotationCollector collector) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));
    collector.print(pw);
    pw.close();
    byte[] data = out.toByteArray();
    out.close();
    return new EmbeddedResource(data, 0);
  }
  
  /**
   * Parse an exception into a string.
   * @param e The exception to parse
   * @return the parsed exception
   */
  public static String parse(Throwable e) {
    StringWriter buffer = new StringWriter();
    PrintWriter pw = new PrintWriter(buffer);
    
    e.printStackTrace(pw);
    
    return (buffer.toString());
  }
}
