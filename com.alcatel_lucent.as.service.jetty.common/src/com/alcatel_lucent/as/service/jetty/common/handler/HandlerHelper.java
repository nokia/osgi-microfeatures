package com.alcatel_lucent.as.service.jetty.common.handler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.ByteArrayISO8859Writer;
import org.eclipse.jetty.util.IO;
import org.eclipse.jetty.util.log.Log;

public class HandlerHelper
{

  private Server server;
  private byte[] favicon;

  private long faviconModified;
  private final static String darkPurple = "#6950A1";
  private boolean showServicePathsOn404;
  
  public HandlerHelper(Server server)
  {
    this.server = server;
    faviconModified=(System.currentTimeMillis()/1000)*1000;
    initFavicon();
  }
  
  public HandlerHelper(Server server, boolean showServicePathsOn404)
  {
    this(server);
    this.showServicePathsOn404 = showServicePathsOn404;
  }

  private void initFavicon() {
    try
    {            
      URL fav = this.getClass().getClassLoader().
      getResource(JwcDefaultHandler.class.getPackage().getName().replace('.', '/') + "/favicon.ico");
      if (fav!=null)
        favicon=IO.readBytes(fav.openStream());
      else { 
        Log.getRootLogger().warn("favicon not found!");
      }
    }
    catch(Exception e)
    {
      Log.getRootLogger().warn(e);
    }
  }

  public boolean hasFavicon() {
    return (favicon != null);
  }
  
  public long getFaviconDate() {
    return faviconModified;
  }
  
  public void insertFavicon(HttpServletResponse response) throws IOException {
    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType("image/x-icon");
    response.setContentLength(favicon.length);
    response.addDateHeader(HttpHeader.LAST_MODIFIED.toString(), faviconModified);
    response.getOutputStream().write(favicon);
  }
  
  public void insertDeployedWebapps(HttpServletResponse response, HttpServletRequest request) throws IOException {
    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    response.setContentType(MimeTypes.Type.TEXT_HTML.toString());

    ByteArrayISO8859Writer writer = new ByteArrayISO8859Writer(1500);

    writer.write("<HTML>\n<HEAD>\n<TITLE>Error 404 - Not Found</TITLE>\n");
    writer.write("<BODY>\n");
    writer.write("<FONT FACE='Verdana, Arial, Helvetica, sans-serif'>");
    writer.write("<CENTER>\n");
    writer.write("<H1>Nokia - Application Server</H1>\n");
    writer.write("<HR color=" + darkPurple + " size=5>\n");
    writer.write("<H2>Error 404 - Not Found</H2>\n");
    //writer.write("No application can match or handle this request<BR>");
    writer.write("<BR>\n");
    writer.write("<HR color=" + darkPurple + " size=3 width='80%'>\n");
    writer.write("</CENTER>\n");
    
    if (showServicePathsOn404) {
    	writer.write("<TABLE align='center' border='0' width='80%'><TR><TD>\n");
    	writer.write("Web applications deployed on this server are:\n");
    	writer.write("</TD></TR><TR><TD>\n");
    	writer.write("<TABLE align='left' border='0'>\n");

    	Handler[] handlers = server==null?null:server.getChildHandlersByClass(ContextHandler.class);
    	for (int i=0;handlers!=null && i<handlers.length;i++)
    	{
    		ContextHandler context = (ContextHandler)handlers[i];
    		writer.write("<TR>");
    		if (context.isRunning())
    		{
    			writer.write("<TD>&bull;&nbsp;<A href=\"");
    			if (context.getVirtualHosts()!=null && context.getVirtualHosts().length>0)
    				writer.write("http://"+context.getVirtualHosts()[0]+":"+request.getLocalPort());
    			writer.write(context.getContextPath());
    			if (context.getContextPath().length()>1 && context.getContextPath().endsWith("/"))
    				writer.write("/");
    			writer.write("\">");
    			writer.write(context.getContextPath());
    			writer.write("</A></TD><TD>");
    			writer.write(context.getDisplayName()==null?"":context.getDisplayName());
    			if (context.getVirtualHosts()!=null && context.getVirtualHosts().length>0)
    				writer.write("&nbsp;@&nbsp;"+context.getVirtualHosts()[0]+":"+request.getLocalPort());
    			writer.write("</TD>\n");
    		}
    		else
    		{
    			writer.write("<TD>&bull;&nbsp;");
    			writer.write(context.getContextPath());
    			if (context.getVirtualHosts()!=null && context.getVirtualHosts().length>0)
    				writer.write("&nbsp;@&nbsp;"+context.getVirtualHosts()[0]+":"+request.getLocalPort());
    			writer.write("</TD><TD>");
    			writer.write(context.getDisplayName());
    			if (context.isFailed())
    				writer.write(" [failed]");
    			if (context.isStopped())
    				writer.write(" [stopped]");
    			writer.write("</TD>\n");
    		}
    		writer.write("</TR>");
    	}
    	writer.write("</TABLE>\n");
    	writer.write("</TD></TR></TABLE>\n");
    }
    
    writer.write("<BR>\n");        
    writer.write("<HR color=" + darkPurple + " size=5>\n");
    writer.write("<TABLE align='center' border='0' width='100%'>\n");
    writer.write("<TR>");
    writer.write("<TD>");
    //writer.write("<P align='left'><SMALL>Jetty-");
    //writer.write(Server.getVersion());
    //writer.write("</P>\n");
    writer.write("</TD>");
    writer.write("<TD>");
    writer.write("<P align='right'><SMALL>&copy; 2018 Nokia. All rights reserved.</SMALL></P>\n");
    writer.write("</TD>");
    writer.write("</TR>");
    writer.write("</TABLE>\n");
    writer.write("</FONT>\n</BODY>\n</HTML>\n");
    writer.flush();
    response.setContentLength(writer.size());
    OutputStream out=response.getOutputStream();
    writer.writeTo(out);
    out.close();
    writer.close();
  }
  
}
