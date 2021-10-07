package com.nextenso.http.agent.engine;

import com.nextenso.http.agent.impl.HttpRequestFacade;
import com.nextenso.http.agent.impl.HttpResponseFacade;
import com.nextenso.proxylet.Proxylet;
import com.nextenso.proxylet.engine.ProxyletContainer;
import com.nextenso.proxylet.engine.ProxyletEngine;
import com.nextenso.proxylet.engine.ProxyletEngineException;
import com.nextenso.proxylet.http.BufferedHttpRequestProxylet;
import com.nextenso.proxylet.http.BufferedHttpRequestPushlet;
import com.nextenso.proxylet.http.BufferedHttpResponseProxylet;
import com.nextenso.proxylet.http.HttpRequestProxylet;
import com.nextenso.proxylet.http.HttpResponseProxylet;
import com.nextenso.proxylet.http.StreamedHttpRequestProxylet;
import com.nextenso.proxylet.http.StreamedHttpResponseProxylet;

public class HttpProxyletEngine extends ProxyletEngine {
  
  public static final int HEADERS = 100;
  public static final int BUFFER = 101;
  public static final int BODY = 102;
  public static final int REQUEST = 103;
  public static final int RESPONSE = 104;
  public static final int MAY_BLOCK_HEADERS = 105;
  public static final int MAY_BLOCK_REQUEST = 106;
  public static final int MAY_BLOCK_RESPONSE = 107;
  public static final int SUSPEND_REQUEST = 108;
  public static final int SUSPEND_RESPONSE = 109;
  public static final int PUSHLET_RESPOND_FIRST = 110;
  public static final int PUSHLET_RESPOND_LAST = 111;
  
  public static final Object ATTR_IGNORE_BODY = new Object();
  private HttpProxyletContainer _container;
  
  public HttpProxyletEngine(HttpProxyletContainer container) {
    _container = container;
  }
  
  public ProxyletContainer getProxyletContainer() {
    return _container;
  }
  
  /**
   * Check if license is valid for all the deployed proxylets.
   * 
   * @throws NoValidLicenseException if at leat one deployed proxylet has no
   *           valid license.
   */
  @Override
  public final void checkLicense()  {

  }
  
  /*********************************************
   * External Calls
   ********************************************/
  
  public int resume(HttpRequestFacade request, int mayBlockValue) throws ProxyletEngineException {
    switch (mayBlockValue) {
    case MAY_BLOCK_HEADERS:
      return handleHeaders(request, true);
    case MAY_BLOCK_REQUEST:
      return handleRequest(request, true);
    case MAY_BLOCK_RESPONSE:
      return handleResponse((HttpResponseFacade) request.getResponse(), true);
    }
    return -1;
  }
  
  public int resume(HttpResponseFacade response, int mayBlockValue) throws ProxyletEngineException {
    switch (mayBlockValue) {
    case MAY_BLOCK_HEADERS:
      return handleHeaders(response, true);
    case MAY_BLOCK_REQUEST:
      return handleRequest((HttpRequestFacade) response.getRequest(), true);
    case MAY_BLOCK_RESPONSE:
      return handleResponse(response, true);
    }
    return -1;
  }
  
  public int getResumeChain(HttpRequestFacade request) {
    HttpProxyletChain chain = _container.getHttpContext().getRequestChain();
    Proxylet pxlet = chain.nextProxylet(request);
    if (pxlet instanceof HttpRequestProxylet)
      return REQUEST;
    return RESPONSE;
  }
  
  /*********************************************
   * Request handling
   ********************************************/
  
  /**
   * Returns HEADERS, BUFFER, REQUEST, RESPONSE, SUSPEND HEADERS: send the
   * headers, then call handleBody on each chunk BUFFER: bufferize the request
   * then call handleBody on the bufferized body REQUEST: send the request -
   * ignore the next body parts RESPONSE: send the response - ignore the next
   * body parts MAY_BLOCK : re-run the method in a specific thread with
   * 'ignoreMayBlock' set to true
   */
  public int handleHeaders(HttpRequestFacade request, boolean ignoreMayBlock) throws ProxyletEngineException {
    // we retrieve the Proxylet chain
    HttpProxyletChain chain = _container.getHttpContext().getRequestChain();
    HttpRequestProxylet proxylet = null;
    try {
      list: while ((proxylet = (HttpRequestProxylet) chain.nextProxylet(request)) != null) {
        int accept = proxylet.accept(request.getProlog(), request.getHeaders());
        boolean ignoreBody = ((accept & 0x02) == 0);
        if (ignoreBody && accept != HttpRequestProxylet.IGNORE)
          accept = accept | 0x02; // we reset IGNORE_BODY
        acc: switch (accept) {
        case HttpRequestProxylet.IGNORE: {
          chain.shift(request, 1);
          break acc;
        }
        case HttpRequestProxylet.ACCEPT_MAY_BLOCK:
          if (!ignoreMayBlock)
            return MAY_BLOCK_HEADERS;
        case HttpRequestProxylet.ACCEPT: {
          if (proxylet instanceof BufferedHttpRequestProxylet
              || proxylet instanceof BufferedHttpRequestPushlet)
            return BUFFER;
          if (!ignoreBody) {
            // we must try to chunk
            // if we cannot chunk, then we bufferize
            if (!request.setStreaming(true))
              return BUFFER;
          }
          int status = ((StreamedHttpRequestProxylet) proxylet).doRequestHeaders(request.getProlog(),
                                                                                 request.getHeaders());
          processedProxylet(proxylet);
          st: switch (status) {
          case StreamedHttpRequestProxylet.SUSPEND: {
            if (!ignoreBody)
              request.addProxylet(proxylet);
            chain.shift(request, 0);
            return SUSPEND_REQUEST;
          }
          case StreamedHttpRequestProxylet.SAME_PROXYLET: {
            if (!ignoreBody)
              request.addProxylet(proxylet);
            chain.shift(request, 0);
            break st;
          }
          case StreamedHttpRequestProxylet.FIRST_PROXYLET: {
            if (!ignoreBody)
              request.addProxylet(proxylet);
            chain.reset(request);
            break st;
          }
          case StreamedHttpRequestProxylet.NEXT_PROXYLET: {
            if (!ignoreBody)
              request.addProxylet(proxylet);
            chain.shift(request, 1);
            break st;
          }
          case StreamedHttpRequestProxylet.LAST_PROXYLET: {
            if (!ignoreBody)
              request.addProxylet(proxylet);
            chain.pad(request);
            break list;
          }
          case StreamedHttpRequestProxylet.REDIRECT_FIRST_PROXYLET: {
            chain.reset(request);
            return handleRequest(request, chain, ignoreMayBlock);
          }
          case StreamedHttpRequestProxylet.REDIRECT_NEXT_PROXYLET: {
            chain.shift(request, 1);
            return handleRequest(request, chain, ignoreMayBlock);
          }
          case StreamedHttpRequestProxylet.REDIRECT_LAST_PROXYLET: {
            return REQUEST;
          }
          case StreamedHttpRequestProxylet.RESPOND_FIRST_PROXYLET: {
            if (!ignoreBody) {
              request.addProxylet(proxylet); // In case the rest of request body comes in
              chain.pad(request);
            }
            HttpResponseFacade response = (HttpResponseFacade) request.getResponse();
            HttpProxyletChain pc = _container.getHttpContext().getResponseChain();
            pc.reset(response);
            return handleResponse(response, pc, ignoreMayBlock);
          }
          case StreamedHttpRequestProxylet.RESPOND_LAST_PROXYLET: {
            if (!ignoreBody) {
              request.addProxylet(proxylet); // In case the rest of request body comes in
              chain.pad(request);
            }
           return RESPONSE;
          }
          default:
            headersError(status, chain, proxylet);
          }
          break acc;
        }
        default:
          acceptError(accept, chain, proxylet);
        }
      }
    } catch (ProxyletEngineException ee) {
      throw ee;
    } catch (Throwable t) {
      throw new ProxyletEngineException(chain, proxylet, t);
    }
    return HEADERS;
  }
  
  /**
   * Returns BODY, REQUEST, RESPONSE BODY: send the body REQUEST: send the
   * request(it was bufferized) - ignore the next body parts RESPONSE: send the
   * response - ignore the next body parts
   */
  public int handleBody(HttpRequestFacade request, boolean isLastChunk, boolean ignoreMayBlock)
      throws ProxyletEngineException {
    // we retrieve the Proxylet chain
    HttpProxyletChain chain = _container.getHttpContext().getRequestChain();
    int size = request.getProxyletsSize();
    for (int i = 0; i < size; i++) {
      StreamedHttpRequestProxylet proxylet = (StreamedHttpRequestProxylet) request.getProxylet(i);
      try {
        proxylet.doRequestBody(request.getBody(), isLastChunk);
      } catch (Throwable t) {
        throw new ProxyletEngineException(chain, proxylet, t);
      }
    }
    
    if (chain.hasMore(request)) {
      // we have a buffered request
      return handleRequest(request, chain, ignoreMayBlock);
    }
    return BODY;
  }
  
  /**
   * Returns REQUEST, RESPONSE, MAY_BLOCK, SUSPEND
   */
  public int handleRequest(HttpRequestFacade request, boolean ignoreMayBlock) throws ProxyletEngineException {
    HttpProxyletChain chain = _container.getHttpContext().getRequestChain();
    return handleRequest(request, chain, ignoreMayBlock);
  }
  
  private int handleRequest(HttpRequestFacade request, HttpProxyletChain chain, boolean ignoreMayBlock)
      throws ProxyletEngineException {
    HttpRequestProxylet proxylet = null;
    try {
      list: while ((proxylet = (HttpRequestProxylet) chain.nextProxylet(request)) != null) {
        int accept = proxylet.accept(request.getProlog(), request.getHeaders());
        boolean ignoreBody = ((accept & 0x02) == 0);
        if (ignoreBody && accept != HttpRequestProxylet.IGNORE)
          accept = accept | 0x02; // we reset IGNORE_BODY
        acc: switch (accept) {
        case HttpRequestProxylet.IGNORE: {
          chain.shift(request, 1);
          continue list;
        }
        case HttpRequestProxylet.ACCEPT_MAY_BLOCK:
          if (!ignoreMayBlock)
            return MAY_BLOCK_REQUEST;
        case HttpRequestProxylet.ACCEPT: {
          if (proxylet instanceof BufferedHttpRequestProxylet) {
            int status = ((BufferedHttpRequestProxylet) proxylet).doRequest(request);
            processedProxylet(proxylet);
            st: switch (status) {
            case BufferedHttpRequestProxylet.SUSPEND: {
              chain.shift(request, 0);
              return SUSPEND_REQUEST;
            }
            case BufferedHttpRequestProxylet.SAME_PROXYLET: {
              chain.shift(request, 0);
              break st;
            }
            case BufferedHttpRequestProxylet.FIRST_PROXYLET: {
              chain.reset(request);
              break st;
            }
            case BufferedHttpRequestProxylet.NEXT_PROXYLET: {
              chain.shift(request, 1);
              break st;
            }
            case BufferedHttpRequestProxylet.LAST_PROXYLET: {
              chain.pad(request);
              break list;
            }
            case BufferedHttpRequestProxylet.RESPOND_FIRST_PROXYLET: {
              HttpResponseFacade response = (HttpResponseFacade) request.getResponse();
              HttpProxyletChain pc = _container.getHttpContext().getResponseChain();
              pc.reset(response);
              return handleResponse(response, pc, ignoreMayBlock);
            }
            case BufferedHttpRequestProxylet.RESPOND_LAST_PROXYLET: {
              return RESPONSE;
            }
            default:
              messageError(status, chain, proxylet);
            }
          } else if (proxylet instanceof StreamedHttpRequestProxylet) {
            int status = ((StreamedHttpRequestProxylet) proxylet).doRequestHeaders(request.getProlog(),
                                                                                   request.getHeaders());
            processedProxylet(proxylet);
            st: switch (status) {
            case StreamedHttpRequestProxylet.SUSPEND: {
              chain.shift(request, 0);
              request.setAttribute(ATTR_IGNORE_BODY, Boolean.valueOf(ignoreBody));
              return SUSPEND_REQUEST;
            }
            case StreamedHttpRequestProxylet.SAME_PROXYLET: {
              if (!ignoreBody)
                ((StreamedHttpRequestProxylet) proxylet).doRequestBody(request.getBody(), true);
              chain.shift(request, 0);
              break st;
            }
            case StreamedHttpRequestProxylet.FIRST_PROXYLET: {
              if (!ignoreBody)
                ((StreamedHttpRequestProxylet) proxylet).doRequestBody(request.getBody(), true);
              chain.reset(request);
              break st;
            }
            case StreamedHttpRequestProxylet.NEXT_PROXYLET: {
              if (!ignoreBody)
                ((StreamedHttpRequestProxylet) proxylet).doRequestBody(request.getBody(), true);
              chain.shift(request, 1);
              break st;
            }
            case StreamedHttpRequestProxylet.LAST_PROXYLET: {
              if (!ignoreBody)
                ((StreamedHttpRequestProxylet) proxylet).doRequestBody(request.getBody(), true);
              chain.pad(request);
              break list;
            }
            case StreamedHttpRequestProxylet.REDIRECT_FIRST_PROXYLET: {
              chain.reset(request);
              break st;
            }
            case StreamedHttpRequestProxylet.REDIRECT_NEXT_PROXYLET: {
              chain.shift(request, 1);
              break st;
            }
            case StreamedHttpRequestProxylet.REDIRECT_LAST_PROXYLET: {
              return REQUEST;
            }
            case StreamedHttpRequestProxylet.RESPOND_FIRST_PROXYLET: {
              if (!ignoreBody) {
                ((StreamedHttpRequestProxylet) proxylet).doRequestBody(request.getBody(), true);
                chain.pad(request);
              }
              HttpResponseFacade response = (HttpResponseFacade) request.getResponse();
              HttpProxyletChain pc = _container.getHttpContext().getResponseChain();
              pc.reset(response);
              return handleResponse(response, pc, ignoreMayBlock);
            }
            case StreamedHttpRequestProxylet.RESPOND_LAST_PROXYLET: {
              if (!ignoreBody) {
                ((StreamedHttpRequestProxylet) proxylet).doRequestBody(request.getBody(), true);
                chain.pad(request);
              }
              return RESPONSE;
            }
            default:
              headersError(status, chain, proxylet);
            }
          } else { // BufferedHttpRequestPushlet
            HttpResponseFacade response = (HttpResponseFacade) request.getResponse();
            PushletOutputStream pushletOS = PushletOutputStream.create(response);
            response.setAttribute(HttpResponseFacade.ATTR_PUSHLET_OS, pushletOS);
            int status;
            status = ((BufferedHttpRequestPushlet) proxylet).doRequest(request);
            processedProxylet(proxylet);
            st: switch (status) {
            case BufferedHttpRequestPushlet.SAME_PROXYLET:
              chain.shift(request, 0);
              break st;
            case BufferedHttpRequestPushlet.FIRST_PROXYLET:
              chain.reset(request);
              break st;
            case BufferedHttpRequestPushlet.RESPOND_FIRST_PROXYLET:
              return PUSHLET_RESPOND_FIRST;
            case BufferedHttpRequestPushlet.RESPOND_LAST_PROXYLET:
              return PUSHLET_RESPOND_LAST;
            default:
              messageError(status, chain, proxylet);
            }
          }
          break acc;
        }
        default:
          acceptError(accept, chain, proxylet);
        }
      }
    } catch (ProxyletEngineException ee) {
      throw ee;
    } catch (Throwable t) {
      throw new ProxyletEngineException(chain, proxylet, t);
    }
    return REQUEST;
  }
  
  /**
   * Resumes a suspended http request. Valid status are: SAME_PROXYLET
   * FIRST_PROXYLET NEXT_PROXYLET LAST_PROXYLET RESPOND_FIRST_PROXYLET
   * RESPOND_LAST_PROXYLET Returns REQUEST RESPONSE
   */
  public int handleRequestResumed(HttpRequestFacade request, int resumeStatus) throws Exception {
    HttpProxyletChain chain = _container.getHttpContext().getRequestChain();
    Proxylet pxlet = chain.nextProxylet(request);
    
    if (pxlet instanceof BufferedHttpRequestProxylet) {
      switch (resumeStatus) {
      case BufferedHttpRequestProxylet.SAME_PROXYLET:
        chain.shift(request, 0);
        return REQUEST;
        
      case BufferedHttpRequestProxylet.FIRST_PROXYLET:
        chain.reset(request);
        return REQUEST;
        
      case BufferedHttpRequestProxylet.NEXT_PROXYLET:
        chain.shift(request, 1);
        return REQUEST;
        
      case BufferedHttpRequestProxylet.LAST_PROXYLET:
        chain.pad(request);
        return REQUEST;
        
      case BufferedHttpRequestProxylet.RESPOND_FIRST_PROXYLET:
        HttpResponseFacade response = (HttpResponseFacade) request.getResponse();
        HttpProxyletChain pc = _container.getHttpContext().getResponseChain();
        pc.reset(response);
        return RESPONSE;
        
      case BufferedHttpRequestProxylet.RESPOND_LAST_PROXYLET:
        return RESPONSE;
        
      default:
        throw new IllegalArgumentException("Could not resume proxylet with status code: " + resumeStatus);
      }
    } else if (pxlet instanceof StreamedHttpRequestProxylet) {
      Boolean b = (Boolean) request.removeAttribute(ATTR_IGNORE_BODY);
      boolean ignoreBody = (b == null) ? true : b.booleanValue();
      switch (resumeStatus) {
      case StreamedHttpRequestProxylet.SAME_PROXYLET:
        if (!ignoreBody)
          ((StreamedHttpRequestProxylet) pxlet).doRequestBody(request.getBody(), true);
        chain.shift(request, 0);
        return REQUEST;
        
      case StreamedHttpRequestProxylet.FIRST_PROXYLET:
        if (!ignoreBody)
          ((StreamedHttpRequestProxylet) pxlet).doRequestBody(request.getBody(), true);
        chain.reset(request);
        return REQUEST;
        
      case StreamedHttpRequestProxylet.NEXT_PROXYLET:
        if (!ignoreBody)
          ((StreamedHttpRequestProxylet) pxlet).doRequestBody(request.getBody(), true);
        chain.shift(request, 1);
        return REQUEST;
        
      case StreamedHttpRequestProxylet.LAST_PROXYLET:
        if (!ignoreBody)
          ((StreamedHttpRequestProxylet) pxlet).doRequestBody(request.getBody(), true);
        chain.pad(request);
        return REQUEST;
        
      case StreamedHttpRequestProxylet.REDIRECT_FIRST_PROXYLET:
        chain.reset(request);
        return REQUEST;
        
      case StreamedHttpRequestProxylet.REDIRECT_NEXT_PROXYLET:
        chain.shift(request, 1);
        return REQUEST;
        
      case StreamedHttpRequestProxylet.REDIRECT_LAST_PROXYLET:
        return REQUEST;
        
      case StreamedHttpRequestProxylet.RESPOND_FIRST_PROXYLET:
        HttpResponseFacade response = (HttpResponseFacade) request.getResponse();
        HttpProxyletChain pc = _container.getHttpContext().getResponseChain();
        pc.reset(response);
        return RESPONSE;
        
      case StreamedHttpRequestProxylet.RESPOND_LAST_PROXYLET:
        return RESPONSE;
        
      default:
        throw new IllegalArgumentException("Could not resume proxylet with status code: " + resumeStatus);
      }
    } else { // BufferedHttpRequestPushlet
      throw new IllegalArgumentException("Pushlet are not resumable");
    }
  }
  
  /*********************************************
   * Response handling
   ********************************************/
  
  /**
   * Returns HEADERS, BUFFER, REQUEST, RESPONSE HEADERS: send the headers, then
   * call handleBody on each chunk BUFFER: bufferize the response then call
   * handleBody on the bufferized body REQUEST: send the request - ignore the
   * next body parts RESPONSE: send the response - ignore the next body parts
   * MAY_BLOCK : re-run the method in a specific thread with 'ignoreMayBlock'
   * set to true
   */
  public int handleHeaders(HttpResponseFacade response, boolean ignoreMayBlock)
      throws ProxyletEngineException {
    // we retrieve the Proxylet chain
    HttpProxyletChain chain = _container.getHttpContext().getResponseChain();
    HttpResponseProxylet proxylet = null;
    try {
      list: while ((proxylet = (HttpResponseProxylet) chain.nextProxylet(response)) != null) {
        int accept = proxylet.accept(response.getProlog(), response.getHeaders());
        boolean ignoreBody = ((accept & 0x02) == 0);
        if (ignoreBody && accept != HttpResponseProxylet.IGNORE)
          accept = accept | 0x02; // we reset IGNORE_BODY
        acc: switch (accept) {
        case HttpResponseProxylet.IGNORE: {
          chain.shift(response, 1);
          break acc;
        }
        case HttpResponseProxylet.ACCEPT_MAY_BLOCK:
          if (!ignoreMayBlock)
            return MAY_BLOCK_HEADERS;
        case HttpResponseProxylet.ACCEPT: {
          if (proxylet instanceof BufferedHttpResponseProxylet)
            return BUFFER;
          if (!ignoreBody) {
            // we must try to chunk
            // if we cannot chunk, then we bufferize
            if (!response.setStreaming(true))
              return BUFFER;
          }
          int status = ((StreamedHttpResponseProxylet) proxylet).doResponseHeaders(response.getProlog(),
                                                                                   response.getHeaders());
          processedProxylet(proxylet);
          st: switch (status) {
          case StreamedHttpResponseProxylet.SUSPEND: {
            if (!ignoreBody)
              response.addProxylet(proxylet);
            chain.shift(response, 0);
            return SUSPEND_RESPONSE;
          }
          case StreamedHttpResponseProxylet.SAME_PROXYLET: {
            if (!ignoreBody)
              response.addProxylet(proxylet);
            chain.shift(response, 0);
            break st;
          }
          case StreamedHttpResponseProxylet.FIRST_PROXYLET: {
            if (!ignoreBody)
              response.addProxylet(proxylet);
            chain.reset(response);
            break st;
          }
          case StreamedHttpResponseProxylet.NEXT_PROXYLET: {
            if (!ignoreBody)
              response.addProxylet(proxylet);
            chain.shift(response, 1);
            break st;
          }
          case StreamedHttpResponseProxylet.LAST_PROXYLET: {
            if (!ignoreBody)
              response.addProxylet(proxylet);
            chain.pad(response);
            break list;
          }
          case StreamedHttpResponseProxylet.REDIRECT_FIRST_PROXYLET: {
            HttpRequestFacade request = (HttpRequestFacade) response.getRequest();
            HttpProxyletChain pc = _container.getHttpContext().getRequestChain();
            pc.reset(request);
            return handleRequest(request, pc, ignoreMayBlock);
          }
          case StreamedHttpResponseProxylet.REDIRECT_LAST_PROXYLET: {
            return REQUEST;
          }
          case StreamedHttpResponseProxylet.RESPOND_FIRST_PROXYLET: {
            chain.reset(response);
            return handleResponse(response, chain, ignoreMayBlock);
          }
          case StreamedHttpResponseProxylet.RESPOND_NEXT_PROXYLET: {
            chain.shift(response, 1);
            return handleResponse(response, chain, ignoreMayBlock);
          }
          case StreamedHttpResponseProxylet.RESPOND_LAST_PROXYLET: {
            return RESPONSE;
          }
          default:
            headersError(status, chain, proxylet);
          }
          break acc;
        }
        default:
          acceptError(accept, chain, proxylet);
        }
      }
    } catch (ProxyletEngineException ee) {
      throw ee;
    } catch (Throwable t) {
      throw new ProxyletEngineException(chain, proxylet, t);
    }
    return HEADERS;
  }
  
  /**
   * Returns BODY, REQUEST, RESPONSE BODY: send the body REQUEST: send the
   * request - ignore the next body parts RESPONSE: send the response(it was
   * bufferized) - ignore the next body parts
   */
  public int handleBody(HttpResponseFacade response, boolean isLastChunk, boolean ignoreMayBlock)
      throws ProxyletEngineException {
    // we retrieve the Proxylet chain
    HttpProxyletChain chain = _container.getHttpContext().getResponseChain();
    int size = response.getProxyletsSize();
    for (int i = 0; i < size; i++) {
      StreamedHttpResponseProxylet proxylet = (StreamedHttpResponseProxylet) response.getProxylet(i);
      try {
        proxylet.doResponseBody(response.getBody(), isLastChunk);
      } catch (Throwable t) {
        throw new ProxyletEngineException(chain, proxylet, t);
      }
    }
    
    if (chain.hasMore(response)) {
      // we have a buffered response
      return handleResponse(response, chain, ignoreMayBlock);
    }
    return BODY;
  }
  
  /**
   * Returns REQUEST, RESPONSE, MAY_BLOCK
   */
  public int handleResponse(HttpResponseFacade response, boolean ignoreMayBlock)
      throws ProxyletEngineException {
    HttpProxyletChain chain = _container.getHttpContext().getResponseChain();
    return handleResponse(response, chain, ignoreMayBlock);
  }
  
  private int handleResponse(HttpResponseFacade response, HttpProxyletChain chain, boolean ignoreMayBlock)
      throws ProxyletEngineException {
    HttpResponseProxylet proxylet = null;
    try {
      list: while ((proxylet = (HttpResponseProxylet) chain.nextProxylet(response)) != null) {
        int accept = proxylet.accept(response.getProlog(), response.getHeaders());
        boolean ignoreBody = ((accept & 0x02) == 0);
        if (ignoreBody && accept != HttpResponseProxylet.IGNORE)
          accept = accept | 0x02; // we reset IGNORE_BODY
        acc: switch (accept) {
        case HttpResponseProxylet.IGNORE: {
          chain.shift(response, 1);
          continue list;
        }
        case HttpResponseProxylet.ACCEPT_MAY_BLOCK:
          if (!ignoreMayBlock)
            return MAY_BLOCK_RESPONSE;
        case HttpResponseProxylet.ACCEPT: {
          if (proxylet instanceof BufferedHttpResponseProxylet) {
            int status = ((BufferedHttpResponseProxylet) proxylet).doResponse(response);
            processedProxylet(proxylet);
            st: switch (status) {
            case BufferedHttpResponseProxylet.SUSPEND: {
              chain.shift(response, 0);
              return SUSPEND_RESPONSE;
            }
            case BufferedHttpResponseProxylet.SAME_PROXYLET: {
              chain.shift(response, 0);
              break st;
            }
            case BufferedHttpResponseProxylet.FIRST_PROXYLET: {
              chain.reset(response);
              break st;
            }
            case BufferedHttpResponseProxylet.NEXT_PROXYLET: {
              chain.shift(response, 1);
              break st;
            }
            case BufferedHttpResponseProxylet.LAST_PROXYLET: {
              chain.pad(response);
              break list;
            }
            case BufferedHttpResponseProxylet.REDIRECT_FIRST_PROXYLET: {
              HttpRequestFacade request = (HttpRequestFacade) response.getRequest();
              HttpProxyletChain pc = _container.getHttpContext().getRequestChain();
              pc.reset(request);
              response.clearContent();
              response.removeHeaders();
              return handleRequest(request, pc, ignoreMayBlock);
            }
            case BufferedHttpResponseProxylet.REDIRECT_LAST_PROXYLET: {
              response.clearContent();
              response.removeHeaders();
              return REQUEST; 
            }
            default:
              messageError(status, chain, proxylet);
            }
          } else {
            int status = ((StreamedHttpResponseProxylet) proxylet).doResponseHeaders(response.getProlog(),
                                                                                     response.getHeaders());
            processedProxylet(proxylet);
            st: switch (status) {
            case StreamedHttpResponseProxylet.SUSPEND: {
              chain.shift(response, 0);
              response.setAttribute(ATTR_IGNORE_BODY, Boolean.valueOf(ignoreBody));
              return SUSPEND_RESPONSE;
            }
            case StreamedHttpResponseProxylet.SAME_PROXYLET: {
              if (!ignoreBody)
                ((StreamedHttpResponseProxylet) proxylet).doResponseBody(response.getBody(), true);
              chain.shift(response, 0);
              break st;
            }
            case StreamedHttpResponseProxylet.FIRST_PROXYLET: {
              if (!ignoreBody)
                ((StreamedHttpResponseProxylet) proxylet).doResponseBody(response.getBody(), true);
              chain.reset(response);
              break st;
            }
            case StreamedHttpResponseProxylet.NEXT_PROXYLET: {
              if (!ignoreBody)
                ((StreamedHttpResponseProxylet) proxylet).doResponseBody(response.getBody(), true);
              chain.shift(response, 1);
              break st;
            }
            case StreamedHttpResponseProxylet.LAST_PROXYLET: {
              if (!ignoreBody)
                ((StreamedHttpResponseProxylet) proxylet).doResponseBody(response.getBody(), true);
              chain.pad(response);
              break list;
            }
            case StreamedHttpResponseProxylet.REDIRECT_FIRST_PROXYLET: {
              HttpRequestFacade request = (HttpRequestFacade) response.getRequest();
              HttpProxyletChain pc = _container.getHttpContext().getRequestChain();
              pc.reset(request);
              response.clearContent();
              response.removeHeaders();
              return handleRequest(request, pc, ignoreMayBlock);
            }
            case StreamedHttpResponseProxylet.REDIRECT_LAST_PROXYLET: {
              response.clearContent();
              response.removeHeaders();
              return REQUEST;
            }
            case StreamedHttpResponseProxylet.RESPOND_FIRST_PROXYLET: {
              chain.reset(response);
              break st;
            }
            case StreamedHttpResponseProxylet.RESPOND_NEXT_PROXYLET: {
              chain.shift(response, 1);
              break st;
            }
            case StreamedHttpResponseProxylet.RESPOND_LAST_PROXYLET: {
              return RESPONSE;
            }
            default:
              headersError(status, chain, proxylet);
            }
          }
          break acc;
        }
        default:
          acceptError(accept, chain, proxylet);
        }
      }
    } catch (ProxyletEngineException ee) {
      throw ee;
    } catch (Throwable t) {
      throw new ProxyletEngineException(chain, proxylet, t);
    }
    return RESPONSE;
  }
  
  /**
   * Resumes a suspended buffered http request. Valid status are: SAME_PROXYLET
   * FIRST_PROXYLET NEXT_PROXYLET LAST_PROXYLET RESPOND_FIRST_PROXYLET
   * RESPOND_LAST_PROXYLET Returns REQUEST RESPONSE
   */
  public int handleResponseResumed(HttpResponseFacade response, int resumeStatus) throws Exception {
    HttpProxyletChain chain = _container.getHttpContext().getResponseChain();
    Proxylet pxlet = chain.nextProxylet(response);
    
    if (pxlet instanceof BufferedHttpResponseProxylet) {
      switch (resumeStatus) {
      case BufferedHttpResponseProxylet.SAME_PROXYLET:
        chain.shift(response, 0);
        return RESPONSE;
        
      case BufferedHttpResponseProxylet.FIRST_PROXYLET:
        chain.reset(response);
        return RESPONSE;
        
      case BufferedHttpResponseProxylet.NEXT_PROXYLET:
        chain.shift(response, 1);
        return RESPONSE;
        
      case BufferedHttpResponseProxylet.LAST_PROXYLET:
        chain.pad(response);
        return RESPONSE;
        
      case BufferedHttpResponseProxylet.REDIRECT_FIRST_PROXYLET:
        HttpRequestFacade request = (HttpRequestFacade) response.getRequest();
        HttpProxyletChain pc = _container.getHttpContext().getRequestChain();
        pc.reset(request);
        return REQUEST;
        
      case BufferedHttpResponseProxylet.REDIRECT_LAST_PROXYLET:
        return REQUEST;
        
      default:
        throw new IllegalArgumentException("Could not resume proxylet with status code: " + resumeStatus);
      }
    } else { // StreamedHttpResponse
      Boolean b = (Boolean) response.removeAttribute(ATTR_IGNORE_BODY);
      boolean ignoreBody = (b == null) ? true : b.booleanValue();
      switch (resumeStatus) {
      case StreamedHttpResponseProxylet.SAME_PROXYLET:
        if (!ignoreBody)
          ((StreamedHttpResponseProxylet) pxlet).doResponseBody(response.getBody(), true);
        chain.shift(response, 0);
        return RESPONSE;
        
      case StreamedHttpResponseProxylet.FIRST_PROXYLET:
        if (!ignoreBody)
          ((StreamedHttpResponseProxylet) pxlet).doResponseBody(response.getBody(), true);
        chain.reset(response);
        return RESPONSE;
        
      case StreamedHttpResponseProxylet.NEXT_PROXYLET:
        if (!ignoreBody)
          ((StreamedHttpResponseProxylet) pxlet).doResponseBody(response.getBody(), true);
        chain.shift(response, 1);
        return RESPONSE;
        
      case StreamedHttpResponseProxylet.LAST_PROXYLET:
        if (!ignoreBody)
          ((StreamedHttpResponseProxylet) pxlet).doResponseBody(response.getBody(), true);
        chain.pad(response);
        return RESPONSE;
        
      case StreamedHttpResponseProxylet.REDIRECT_FIRST_PROXYLET:
        HttpRequestFacade request = (HttpRequestFacade) response.getRequest();
        HttpProxyletChain pc = _container.getHttpContext().getRequestChain();
        pc.reset(request);
        return REQUEST;
        
      case StreamedHttpResponseProxylet.REDIRECT_LAST_PROXYLET:
        return REQUEST;
        
      case StreamedHttpResponseProxylet.RESPOND_FIRST_PROXYLET:
        chain.reset(response);
        return RESPONSE;
        
      case StreamedHttpResponseProxylet.RESPOND_NEXT_PROXYLET:
        chain.shift(response, 1);
        return RESPONSE;
        
      case StreamedHttpResponseProxylet.RESPOND_LAST_PROXYLET:
        return RESPONSE;
        
      default:
        throw new IllegalArgumentException("Could not resume proxylet with status code: " + resumeStatus);
      }
    }
  }
  
  private void acceptError(int accept, HttpProxyletChain chain, Proxylet proxylet)
      throws ProxyletEngineException {
    String text = "Invalid value returned from method accept: " + accept;
    throw new ProxyletEngineException(chain, proxylet, text);
  }
  
  private void headersError(int status, HttpProxyletChain chain, Proxylet proxylet)
      throws ProxyletEngineException {
    String text = "Invalid return code from headers processing: " + status;
    throw new ProxyletEngineException(chain, proxylet, text);
  }
  
  private void messageError(int status, HttpProxyletChain chain, Proxylet proxylet)
      throws ProxyletEngineException {
    String text = "Invalid return code from message processing: " + status;
    throw new ProxyletEngineException(chain, proxylet, text);
  }
}
