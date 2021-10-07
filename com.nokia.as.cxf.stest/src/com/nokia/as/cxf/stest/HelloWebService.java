/*
 ****************************************************************************
 *
 * Copyright (c) 2016 Nokia. All rights reserved.
 * Please read the associated COPYRIGHTS file for more details.
 *
 ****************************************************************************
 */
package com.nokia.as.cxf.stest;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.*;
import javax.xml.ws.*;
import javax.xml.ws.handler.*;
import javax.annotation.*;
import org.osgi.service.component.annotations.*;


@Component(service = { Object.class }, property = {
        "tpapps.ws=true",
        "tpapps.ws.path=/hello"
})
@WebService
public class HelloWebService {

    @Resource
    private WebServiceContext context;

    @Activate
    void start() {
	Thread t = new Thread(() -> {
		try { Thread.sleep(3000); } catch (Exception e) {}
		System.out.println("XX: activate: context=" + context);
	});
	t.start();
    }
    
    @WebMethod
    public int doubleAnInteger ( int numberToDouble ) {
	System.out.println("HelloWebService.doubleAnInteger: " + numberToDouble);
	return 999;
    }

}
