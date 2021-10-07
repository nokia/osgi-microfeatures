package com.alcatel_lucent.as.management.bnd;

import java.util.Iterator;
import java.util.Map;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.AnalyzerPlugin;
import aQute.bnd.service.Plugin;
import aQute.service.reporter.Reporter;

/**
 * This class is a BND plugin. It scans the target bundle and look for MBD annotations.
 * It can be directly used when using bnd/ant and can be referenced inside the ".bnd" descriptor, using
 * the "-plugin" parameter.
 */
public class AnnotationPlugin implements AnalyzerPlugin, Plugin {
  private boolean _debug;

  @Override
  public void setProperties(Map<String, String> map) {
    _debug = "true".equals(map.get("debug"));
  }

  @Override
  public void setReporter(Reporter reporter) {
  }

  /**
   * This plugin is called after analysis of the JAR but before manifest
   * generation. When some BND annotations are found, the plugin will add the corresponding 
   * mbeans-descriptors under META-INF/ directory.
   * 
   * @param analyzer the object that is used to retrieve classes containing DM annotations.
   * @return true if the classpace has been modified so that the bundle classpath must be reanalyzed
   * @throws Exception on any errors.
   */
  public boolean analyzeJar(Analyzer analyzer) throws Exception {
    analyzer.setTrace(_debug);

    try {
      // We'll do the actual parsing using a DescriptorGenerator object.
      DescriptorGenerator generator = new DescriptorGenerator(analyzer);
      generator.setTrace(_debug);
      if (generator.execute()) {
        // We have parsed some annotations: set the OSGi "X-AS-MBeansDescriptors" header in the target bundle.
        analyzer.setProperty("X-AS-MBeansDescriptors", generator.getDescriptorPaths());

        // And insert the generated descriptors into the target bundle.
        Map<String, Resource> resources = generator.getDescriptors();
        for (Map.Entry<String, Resource> entry : resources.entrySet()) {
          analyzer.getJar().putResource(entry.getKey(), entry.getValue());
        }
      }

      // Check if some warnings have to be logged.
      if (analyzer.getWarnings().size() != 0) {
        for (Iterator<String> e = analyzer.getWarnings().iterator(); e.hasNext();) {
          System.out.println(e.next());
        }
      }
    } catch (Throwable err) {
      err.printStackTrace();
      System.exit(1);
    }

    return false;
  }
}
