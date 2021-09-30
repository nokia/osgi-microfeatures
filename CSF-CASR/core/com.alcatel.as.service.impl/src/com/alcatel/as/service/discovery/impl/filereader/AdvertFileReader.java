package com.alcatel.as.service.discovery.impl.filereader;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferenceCardinality;

import com.alcatel.as.service.discovery.Advertisement;
import com.alcatel.as.util.config.ConfigConstants;
import com.alcatel.as.util.config.ConfigHelper;
import com.alcatel_lucent.as.management.annotation.config.Config;
import com.alcatel_lucent.as.management.annotation.config.FileDataProperty;

@Component(configurationPolicy = ConfigurationPolicy.REQUIRE)
@Config(section="Advertisement File Reader")
public class AdvertFileReader {
  @FileDataProperty(title="Advertisements File",
      help="This property contains the list of addresses to be advertised to remote components",
      fileData="fileAdvertReader.xml",
      required=true)
  private final static String CONF = "fileAdvertReader.advertFile";

  public static final Logger LOGGER = Logger.getLogger("as.service.advert.filereader");
  private Dictionary<String, String> _system;
  private Map<String, ServiceRegistration> _adverts = new HashMap<> ();

  @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC, target="(service.pid=system)")
  void bindSystemConfig(Dictionary<String, String> system) {
    _system = system;
  }

  void unbindSystemConfig(Dictionary<String, String> system) {
  }

  @Modified
  void modified (Map<String, String> conf, BundleContext bctx) {
    List<ServiceRegistration> regs = register(bctx, ConfigHelper.getString(conf, CONF, ""), _system);
    if (regs == null) return;
    List<String> remove = new ArrayList<> ();
    for (String key : _adverts.keySet ()){
      ServiceRegistration reg = _adverts.get (key);
      if (regs.contains (reg) == false)
	remove.add (key);
    }
    for (String key : remove){
      _adverts.remove (key).unregister ();
      if (LOGGER.isInfoEnabled ())
	LOGGER.info("Un-Registering Advertisement : " + key);
    }
  }
  
  @Activate
  void start(Map<String, String> conf, BundleContext bctx) {
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("Starting FileAdvertisementLoader component with conf " + conf);
    }
    register(bctx, ConfigHelper.getString(conf, CONF, ""), _system);
  }
  
  public List<ServiceRegistration> register(BundleContext osgi, String xml, Dictionary<String, String> conf) {
    try {
      if (xml == null || (xml=xml.trim()).length() == 0)
        return new ArrayList<ServiceRegistration> ();
      if (LOGGER.isDebugEnabled())
        LOGGER.debug("Ready to parse : " + xml);
      Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new java.io.StringReader (xml)));
      Element root = doc.getDocumentElement();
      List<String[]> defProps = parseProperties(root, conf);
      NodeList adverts = root.getElementsByTagName("advertisement");
      List<ServiceRegistration> regs = new ArrayList<>();
      for (int i = 0; i < adverts.getLength(); i++)
        regs.add(register(osgi, (Element) adverts.item(i), defProps, conf));
      return regs;
    } catch (Throwable t) {
      LOGGER.error ("Exception while reading adverts", t);
      return null;
    }
  }
  
  private ServiceRegistration register(BundleContext osgi, Element advertNode, List<String[]> defProp,
                                       Dictionary<String, String> conf) throws Exception {
    String ip = advertNode.getAttributes().getNamedItem("ip").getNodeValue();
    int port = Integer.parseInt(advertNode.getAttributes().getNamedItem("port").getNodeValue());
    String key = ip+" "+port;
    ServiceRegistration sr = _adverts.get (key);
    if (sr != null) return sr;
    Advertisement advert = new Advertisement(ip, port);
    Hashtable options = new Hashtable();
    for (String[] props : parseProperties(advertNode, conf))
      options.put(props[0], props[1]);
    for (String[] props : defProp)
      if (options.get(props[0]) == null)
        options.put(props[0], props[1]);
    addDefaultProperties(options);
    if (LOGGER.isInfoEnabled())
      LOGGER.info("Registering new Advertisement : " + advert + " with properties=" + options);
    sr = osgi.registerService(Advertisement.class.getName(), advert, options);
    _adverts.put (key, sr);
    return sr;
  }
  
  private void addDefaultProperties(Hashtable options) {
	  if (options.get("provider") == null) {
		  options.put("provider", "fileadvert");
	  }
	  if (options.get(ConfigConstants.PLATFORM_NAME) == null) {
		  String platformName = _system.get(ConfigConstants.PLATFORM_NAME);
		  if (platformName != null) {
			  options.put(ConfigConstants.PLATFORM_NAME, platformName);
		  }
	  }
  }

  private static List<String[]> parseProperties(Element root, Dictionary<String, String> conf) throws Exception {
    NodeList props = root.getElementsByTagName("property");
    List<String[]> list = new ArrayList<>();
    for (int i = 0; i < props.getLength(); i++) {
      Node prop = props.item(i);
      if (prop.getParentNode () != root) continue;
      list.add(parseProperty(prop, conf));
    }
    return list;
  }
  
  private static String[] parseProperty(Node prop, Dictionary<String, String> conf) throws Exception {
    return new String[] { prop.getAttributes().getNamedItem("name").getNodeValue(),
        resolve(prop.getTextContent(), conf) };
  }
  
  private static String resolve(String value, Dictionary<String, String> conf) {
    try {
      if (value.length() == 0)
        return value;
      int start = value.indexOf('%');
      if (start == -1)
        return value;
      int stop = value.indexOf('%', start + 1);
      String k = value.substring(start + 1, stop);
      String v = conf.get(k);
      if (v == null)
        v = "";
      if ((stop + 1) == value.length()) {
        return value.substring(0, start) + v;
      } else {
        return value.substring(0, start) + v + resolve(value.substring(stop + 1), conf);
      }
    } catch (Throwable t) {
      throw new IllegalArgumentException("Invalid value : " + value);
    }
  }
}
