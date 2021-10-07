package com.nextenso.proxylet.admin;

import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Attributes;
import java.util.concurrent.atomic.AtomicReference;
import java.net.JarURLConnection;
import java.io.StringWriter;
import java.io.InputStream;
import java.io.ByteArrayInputStream;

import org.apache.log4j.Logger;

import static org.osgi.framework.Constants.BUNDLE_NAME;
import static org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME;
import static org.osgi.framework.Constants.BUNDLE_VERSION;

import com.alcatel.as.management.blueprint.Blueprint;
import com.alcatel.as.management.platform.ConfigManager;
import com.alcatel.as.management.platform.CreateInstancePlugin;
import com.alcatel.as.service.metatype.InstanceProperties;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * base class used by subagents.
 * loads META-INF/[protocol].xml from bundles
 * and aggregates the contents to each subagent configuration
 */
public class BearerAggregator implements CreateInstancePlugin {
  static final Logger logger = Logger.getLogger("as.proxylet.admin");
  
  final Bearer.Factory _bearerFactory;
  final String _pid, _property;
  final DocumentBuilder _builder;
  
  /**
   * @param protocol the proxylets protocol
   * @param pid propertiesID containing the proxylets config
   * @param property property name for the proxylets config
   */
  public BearerAggregator(Bearer.Factory bf, String pid, String property) throws Exception {
    _bearerFactory = bf;
    _pid = pid;
    _property = property;
    
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setValidating(false);
    dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    _builder = dbf.newDocumentBuilder();
  }
  
  /**@deprecated*/
  public void bindConfigManager(ConfigManager mgr) { }
  
  public String getPid() { return _pid; }
  
  public String getProperty() { return _property; }
  
  public Object begin(ConfigManager.Tx tx) throws Exception 
  {
    AtomicReference<Bearer> bearer = new AtomicReference<Bearer>(); //used a simple value container
    String previous = tx.hasProperty(_pid + "/" + _property) 
      ? tx.getProperty(_pid + "/" + _property) 
      : null;
    if (previous == null || previous.trim().length() == 0) 
    {
      return bearer;
    }
    InputStream in = new ByteArrayInputStream(previous.getBytes());
    try 
    {
      bearer.set(_bearerFactory.newBearer(_builder.parse(in).getDocumentElement()));
      if (logger.isDebugEnabled())
        logger.debug("loaded original " + _bearerFactory.getProtocol() + ".bearer for " + tx.component()
            + ": " + bearer.get().toXML());
    } 
    catch (Exception e) 
    {
      logger.warn("failed to load previous proxylets: " + previous, e);
    } 
    finally 
    {
      if (in != null) try { in.close(); } catch (Exception e) { logger.warn("", e); }
    }
    return bearer;
  }
  
  public void scan(JarURLConnection jar, ConfigManager.Tx tx, Object ctx) throws Exception 
  {
    JarFile jf = jar.getJarFile();
    JarEntry je = null;
    String protocol = _bearerFactory.getProtocol().toString().toLowerCase();
    AtomicReference<Bearer> ref = (AtomicReference<Bearer>)ctx;

    if ((je = jf.getJarEntry("WEB-INF/" + protocol + ".xml")) != null
        || (je = jf.getJarEntry("WEB-INF/" + protocol.toUpperCase() + ".xml")) != null) 
    {
      if (logger.isInfoEnabled())
        logger.info("found " + je.getName() + " in " + jar.getJarFileURL());

      Blueprint bp = tx.blueprint();
      if (bp != null)
      {
	  String filter = bp.getFilter();
	  String bname = jf.getManifest().getMainAttributes().getValue(BUNDLE_NAME);
	  String bsname = jf.getManifest().getMainAttributes().getValue(BUNDLE_SYMBOLICNAME);
	  
	  if ((filter != null) && 
	      (filter.indexOf("="+bname+")") < 0) &&
	      (filter.indexOf("="+bsname+")") < 0)) {
	      /* Manage assembly. this bsname can be in the required bundles */
	      String requiredBundles = bp.toString();
	      if ((requiredBundles == null) || 
		  (requiredBundles.indexOf("/"+bsname+"-") < 0)) {
		  logger.info("Ignore "+bsname+" not explicitely required by filter="+filter+" nor in required bundles="+requiredBundles);
		  return;
	      }
	  }
      }

      InputStream in = jf.getInputStream(je);
      try 
      {
        Document doc = _builder.parse(in);
        Bearer newBearer = _bearerFactory.newBearer(doc.getDocumentElement());
        Attributes mf = jf.getManifest().getMainAttributes();
        newBearer.setProxyletSetElements(
            mf.getValue(BUNDLE_NAME)
           +"_"
           +mf.getValue(BUNDLE_VERSION));


        if (ref.get() == null)  //re-assign ctx!
          ref.set(newBearer);
        else ref.get().addProxyletSetElements(newBearer);
      } 
      catch (Exception e) 
      {
        logger.warn("failed to load " + je.getName() + " from " + jar.getJarFileURL(), e);
      } 
      finally 
      {
        if (in != null) try { in.close(); } catch (Exception e) { logger.warn("", e); }
      }
      if (logger.isDebugEnabled())
        logger.debug(protocol + ".bearer updated to " + ref.get().toXML());
    }
  }
  
  public void end(ConfigManager.Tx tx, Object ctx) throws Exception 
  {
    Bearer bearer = ((AtomicReference<Bearer>)ctx).get();
    if (bearer != null && tx.hasProperty(_pid + "/" + _property)) 
    {
      tx.setProperty(_pid + "/" + _property, bearer.toXML());
    } 
    else if (logger.isDebugEnabled())
      logger.debug("No proxylets found for " + _bearerFactory.getProtocol());
  }

  public boolean supportUpdate() {return false;}
}
