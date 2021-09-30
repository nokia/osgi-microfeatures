package com.nokia.as.h2client.test.server;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.nokia.as.service.hkdf.HkdfService;

@Component(service = TestService.class)
@Path("test")
public class TestService {

  static volatile String RESP;
  static volatile Logger logger = Logger.getLogger("test");

  @GET
  @Path("generator")
  @Produces("text/plain")
  public String generator(@QueryParam("size") int size, @QueryParam("update") String update,
      @HeaderParam("string-to-replicate") String string_to_replicate) {
    if (RESP == null || update != null) {
      RESP = generateResp(size, string_to_replicate);
    }

    return RESP;
  }

  @POST
  @Path("echo")
  @Produces(MediaType.TEXT_PLAIN)
  @Consumes(MediaType.APPLICATION_JSON)
//    @Consumes(MediaType.APPLICATION_FORM_URLENCODED) //(final MultivaluedMap<String, String> params)
  public String echo(@Context UriInfo uriInfo, @Context HttpHeaders httpHeaders, InputStream stream) {
    return httpHeaders.getRequestHeaders().toString() + "\n" + uriInfo.getQueryParameters().toString() + "\n"
        + new BufferedReader(new InputStreamReader(stream)).lines().collect(Collectors.joining("\n"));
  }

  @DELETE
  @Path("generator")
  public Response delete() {

    logger.warn("DELETE ");

    return Response.noContent().build();
  }

  private String generateResp(int size, String string_to_replicate) {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < size; i++) {
      sb.append(string_to_replicate);
    }
    return sb.toString();
  }

  public static final String keymap = "tcp.secure.keyexport";
  public static final String key = "tcp.secure.keyexport.master_secret";

  private HkdfService _hkdf_service;

  @Reference
  public void setHkdfService(HkdfService hkdf_service) {
    _hkdf_service = hkdf_service;
  }

  @GET
  @Produces("text/plain")
  @Path("tlsexport")
  public String hello(@Context ContainerRequestContext ctx) {
    Map<String, Object> keying_material = (Map<String, Object>) ctx.getProperty(keymap);
    if (_hkdf_service != null) {
      byte[] result = _hkdf_service.expand(keying_material, 64, "N32", "N32-1234567", "parallel_request_key");
      System.out.println("HKDF: " + new java.math.BigInteger(1, result).toString(16));
    } else {
      System.err.println("HKDF: null service");
    }

    String key_txt = new java.math.BigInteger(1, ((byte[]) ((Map<String, Object>) ctx.getProperty(keymap)).get(key)))
        .toString(16);
    return "Key is " + key_txt + "\n" + ctx.getProperty("tcp.secure.keyexport");
  }

}
