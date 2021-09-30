package com.alcatel.as.agent.http.stest.jaxrs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.*;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.Request;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.log4j.Logger;
import org.glassfish.jersey.media.multipart.FormDataParam;

@Component(provides=TestJAXRSBean.class)
@Path("helloworld")
public class TestJAXRSBean {
    public static final String MESSAGE = "Hello Proxylet World";
    final static Logger LOG = Logger.getLogger(TestJAXRSBean.class);
    
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getHello() {
        return MESSAGE;
    }


    @Path("delete")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public String deleteHello() {
      return "Hello DELETE";
    }


    @Path("head")
    @HEAD
    @Produces(MediaType.TEXT_PLAIN)
    public String headHello() {
      return "Hello HEAD";
    }
    
    
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("get")
    public String get() {
        return MESSAGE;
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("timeout")
    public String getHello(@HeaderParam("X-sleep-duration") String sleepTimeAttr) {
    	int sleepTime = sleepTimeAttr == null ? 5000 : Integer.parseInt(sleepTimeAttr);
    	try {
    		System.out.println("sleeping " + sleepTime);
    		Thread.sleep(sleepTime);
    	} catch (Exception e) {
			// TODO: handle exception
		}
        return MESSAGE;
        
    }

    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @PUT
    @Path("test2")
    public String postOrPutHello(@Context Request request, @HeaderParam("X-Alexa") String testHeader, @FormDataParam("my_file") InputStream uploadedInputStream) {
      
        StringBuilder sb = new StringBuilder();
        sb.append("header OK: ").append("play-despacito".equals(testHeader)).append("\n");
        
        long fileSize = 0;
        try {
          while(uploadedInputStream.read() != -1) fileSize++;
        } catch (IOException e) {
          fileSize = - 1;
        }
        
        sb.append("File size: ")
        	.append(fileSize)
        	.append("\n")
          .append("Method: ")
          .append(request.getMethod())
          .append("\n");
        return sb.toString();
    }
    
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public String postHello(@HeaderParam("X-Alexa") String testHeader, @FormDataParam("my_file") InputStream uploadedInputStream) {
      
        StringBuilder sb = new StringBuilder();
        sb.append("header OK: ").append("play-despacito".equals(testHeader)).append("\n");
        
        long fileSize = 0;
        try {
          while(uploadedInputStream.read() != -1) fileSize++;
        } catch (IOException e) {
          fileSize = - 1;
        }
        
        sb.append("File size: ")
        	.append(fileSize)
        	.append("\n");
        return sb.toString();
    }
    
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Path("big")
    public Response getBigResponseWithBigPayload(@HeaderParam("X-Alexa") String testHeader, @FormDataParam("my_file") InputStream uploadedInputStream) {
      byte[] myChunk = new byte[1024];
      for(int i = 0 ; i < myChunk.length; i++) {
        myChunk[i] = 'A';
      }
      StreamingOutput out = new StreamingOutput() {
        
        @Override
        public void write(OutputStream arg0) throws IOException, WebApplicationException {

          for(int i = 0; i < 64 ; i++) {
            arg0.write(myChunk);
            arg0.flush();
          }
          
          StringBuilder sb = new StringBuilder();
          sb.append("header OK: ").append("play-despacito".equals(testHeader)).append("\n");
          
          long fileSize = 0;
          if(uploadedInputStream != null) {
            try {
              while(uploadedInputStream.read() != -1) fileSize++;
            } catch (IOException e) {
              fileSize = - 1;
            }
          } 
          
          sb.append("File size: ")
            .append(fileSize)
            .append("\n");
          
          arg0.write(sb.toString().getBytes());
          arg0.flush();
          
        }
      };
            
      return Response.ok(out).build();
    }
    
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("big")
    public Response getBigResponse() {
      byte[] myChunk = new byte[1024];
      for(int i = 0 ; i < myChunk.length; i++) {
        myChunk[i] = 'A';
      }
      StreamingOutput out = new StreamingOutput() {
        
        @Override
        public void write(OutputStream arg0) throws IOException, WebApplicationException {
          
          
          for(int i = 0; i < 64 ; i++) {
            arg0.write(myChunk);
            arg0.flush();
          }
        }
      };
            
      return Response.ok(out).build();
    }
    

}