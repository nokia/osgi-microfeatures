// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.http2.stest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.felix.dm.annotation.api.Component;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;


@Component(provides=TestJAXRSBean.class)
@Path("helloworld")
public class TestJAXRSBean {
    public static final String MESSAGE = "Hello World! 222";

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getHello() {
        return MESSAGE;
    }
    
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Path("post2")
    public String getTestPost() {
        return "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    }
    
    
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Path("post")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public String getTestPost2( @FormDataParam("my_file") InputStream uploadedInputStream,
        @FormDataParam("my_file") FormDataContentDisposition fileDetail) {
        System.out.println(fileDetail.getFileName() + " size " + fileDetail.getSize());
        try (BufferedReader br = new BufferedReader(new InputStreamReader(uploadedInputStream, Charset.defaultCharset()))) {
          return br.lines().count() + " lines received";
        } catch (IOException e) {
          e.printStackTrace();
        }
        
        return "nothing";
    }
}