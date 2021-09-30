package com.alcatel.as.http2.client.api.impl ;

import com.alcatel.as.http2.client.api.HttpResponse.ResponseInfo;
import com.alcatel.as.http2.client.api.HttpHeaders;

import java.util.Map;

import com.alcatel.as.http2.client.api.HttpClient;

class ResponseInfoImpl implements ResponseInfo {
    private final int statusCode;
    private final HttpHeaders headers;
    private final HttpClient.Version version;
    private final Map<String, Object> keyingMaterial;

//    ResponseInfoImpl(Response response) {
//        this.statusCode = response.statusCode();
//        this.headers = response.headers();
//        this.version = response.version();
//    }
//
    ResponseInfoImpl(int statusCode, HttpHeaders headers, HttpClient.Version version, Map<String, Object> keyingMaterial) {
        this.statusCode = statusCode;
        this.headers = headers;
        this.version = version;
        this.keyingMaterial = keyingMaterial;
    }

    /**
     * Provides the response status code
     * @return the response status code
     */
    public int statusCode() {
        return statusCode;
    }

    /**
     * Provides the response headers
     * @return the response headers
     */
    public HttpHeaders headers() {
        return headers;
    }

    /**
     * provides the response protocol version
     * @return the response protocol version
     */
    public HttpClient.Version version() {
        return version;
    }

    public Map<String, Object> exportKeyingMaterial() {
        return keyingMaterial;
    }
}
