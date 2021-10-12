// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.util.httploader.server;
 
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.log4j.Logger;
 
 
 
/**
* Our simple SpellChecker web service.
*/
@Component(provides = Server.class)
@Path("hello")
public class Server {
                final static Logger _log = Logger.getLogger("as.service.metering");
     static volatile String RESP;
    
     
     @GET
     @Path("/get")
     @Produces("text/plain")
     public String hello() {
                
                 //_log.warn("TROUVE");
                 
         return "YES";
     }
     
 
     
     @POST
     @Path("/post")
     @Produces("text/plain")
     @Consumes("application/x-www-form-urlencoded")
     public String post(@FormParam("id") String id) { 
                 
                 _log.warn("HOME =" + id);
                 return "Hello ";
                 
     }
     
     @PUT
     @Path("/put")
     @Produces("text/plain")
     @Consumes("application/x-www-form-urlencoded")
     public String put(@FormParam("id") String id) { 
                 
                 _log.warn("HOME =" + id);
                 return "Hello ";
                 
     }
     @DELETE
     @Path("/delete")
     @Produces("text/plain")
     @Consumes("application/x-www-form-urlencoded")
     public String delete() { 
                 
                 _log.warn("DELETE");
                 return "DELETE";
                 
     }
     
     
     private String generateResp(int size) {
         StringBuffer sb = new StringBuffer();
         for (int i = 0; i < size; i ++) {
             sb.append('X');
         }
         return sb.toString();
    }
}
