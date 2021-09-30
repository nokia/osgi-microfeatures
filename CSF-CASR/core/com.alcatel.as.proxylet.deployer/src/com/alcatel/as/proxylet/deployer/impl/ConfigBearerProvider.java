package com.alcatel.as.proxylet.deployer.impl;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.util.Dictionary;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.apache.log4j.Logger;

import com.nextenso.proxylet.admin.Bearer;
import com.nextenso.proxylet.admin.Protocol;
import com.nextenso.proxylet.engine.DeployerDescriptor;

import com.alcatel.as.management.platform.ConfigManager;
import static com.alcatel.as.util.config.ConfigConstants.*;

public class ConfigBearerProvider implements BearerProvider 
{
  protected Bearer _bearer;

  /**
   * @param bf the protocol specific Bearer.Factory
   * @param bearerXml the bearer String value from agent config
   */
  public ConfigBearerProvider(DeployerDescriptor dd)
    throws Exception
  {
    if (dd.getProxyletsConfiguration() == null || dd.getProxyletsConfiguration().trim().length() == 0) return;

    Logger logger = Logger.getLogger("as.service.pxletdeployer."+dd.getProtocol().toLowerCase());

    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setValidating(false);
    dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    DocumentBuilder builder = dbf.newDocumentBuilder();

    InputStream in = new ByteArrayInputStream(dd.getProxyletsConfiguration().getBytes());
    try 
    {
      Document doc = builder.parse(in);
      _bearer = dd.getBearerFactory().newBearer(doc.getDocumentElement()); 
    }
    catch(Exception e)
    {
      logger.warn("cannot reload saved Bearer", e);
      _bearer = null;
    }
    finally 
    {
      if (in != null) try { in.close(); } catch(Exception e) { logger.warn("IO error", e); }
    }
  }

  @Override public Bearer readDeployedBearerContext() throws Exception 
  {
    return _bearer;
  }
}
