// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.http;

import java.net.MalformedURLException;

import com.alcatel.as.util.serviceloader.ServiceLoader;

/**
 * This class is used to create a new instance of HttpClient.
 * <p/>
 * The name of the class used for the HttpClient is taken from the System property
 * <i>com.nextenso.proxylet.http.HttpClient.class</i>
 */
public abstract class HttpClientFactory
{
  /**
   * Gets the HttpClientFactory service.
   * 
   * @return the HttpClientFactory service.
   */
  public static HttpClientFactory getInstance()
  {
    HttpClientFactory factory =
        (HttpClientFactory) ServiceLoader.getService(HttpClientFactory.class.getName());
    if (factory == null)
    {
      throw new RuntimeException("Http Client Factory service not available");
    }
    return factory;
  }

  /**
   * Returns a new instance of the current HttpClient implementation.
   * 
   * @param url the url that will be retrieved by the HttpClient.
   * @return a new instance of HttpClient or <code>null</code> if an instanciation problem
   *         occurred.
   * @throws MalformedURLException if the provided url is not valid.
   */
  public abstract HttpClient create(String url) throws MalformedURLException;

  /**
   * Returns a new instance of the current HttpClient implementation. The returned client checks
   * license validity on output flow. That is a proxylet using the returned client must have a
   * valid license to be able to send request.
   * 
   * @param url the url that will be retrieved by the HttpClient.
   * @param callerId the id of the componant using the HttpClient.
   * @return a new instance of HttpClient or <code>null</code> if an instanciation problem
   *         occurred.
   * @throws MalformedURLException if the provided url is not valid.
   * @internal
   */
  public abstract HttpClient create(String url, String callerId) throws MalformedURLException;

  /**
   * Returns a new instance of the current HttpClient implementation. The returned client reuses
   * elements of the given HttpClient instance, such as credentials and callerId if any
   * 
   * @param url the url that will be retrieved by the HttpClient.
   * @param client the HttpClient instance to copy from
   * @return a new instance of HttpClient or <code>null</code> if an instanciation problem
   *         occurred.
   * @throws MalformedURLException if the provided url is not valid.
   * @internal
   */
  public abstract HttpClient create(String url, HttpClient client) throws MalformedURLException;

  /**
   * Returns a new instance of the current HttpClient implementation.
   * 
   * @param url the url that will be retrieved by the HttpClient.
   * @return a new instance of HttpClient or <code>null</code> if an instanciation problem
   *         occurred.
   * @throws MalformedURLException if the provided url is not valid.
   * @deprecated use {@link #create(String)} method.
   */
  public static HttpClient newHttpClient(String url) throws MalformedURLException
  {
    return getInstance().create(url);
  }

  /**
   * Returns a new instance of the current HttpClient implementation. The returned client reuses
   * elements of the given HttpClient instance, such as credentials and callerId if any
   * 
   * @param url the url that will be retrieved by the HttpClient.
   * @param client the HttpClient instance to copy from
   * @return a new instance of HttpClient or <code>null</code> if an instantiation problem
   *         occurred.
   * @throws MalformedURLException if the provided url is not valid.
   * @internal
   * @deprecated use {@link #create(String, HttpClient)} instead
   */
  public static HttpClient newHttpClient(String url, HttpClient client)
    throws MalformedURLException
  {
    return getInstance().create(url, client);
  }

  /**
   * Returns a new instance of the current HttpClient implementation. The returned client checks
   * license validity on output flow. That is a proxylet using the returned client must have a
   * valid license to be able to send request.
   * 
   * @param url the url that will be retrieved by the HttpClient.
   * @param callerId the id of the component using the HttpClient.
   * @return a new instance of HttpClient or <code>null</code> if an instanciation problem
   *         occurred.
   * @throws MalformedURLException if the provided url is not valid.
   * @internal
   * @deprecated use {@link #create(String, String)}.
   */
  public static HttpClient newHttpClient(String url, String callerId)
    throws MalformedURLException
  {
    return getInstance().create(url, callerId);
  }

  /** Deprecated: this property is not used anymore (SPI META-INF/services are used instead) */
  public static final String HTTP_CLIENT_CLASS = "com.nextenso.proxylet.http.HttpClient.class";
}
