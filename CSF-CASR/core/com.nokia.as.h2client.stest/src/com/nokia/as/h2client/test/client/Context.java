package com.nokia.as.h2client.test.client;

import com.alcatel.as.http2.client.api.HttpClientFactory;
import org.apache.log4j.Logger;

public class Context implements Http2ClientTestDescriptor.Context {

    public Context(HttpClientFactory _factory, Http2ClientTestDescriptor _test) {
        this._factory = _factory;
        this._test = _test;
        this._logger = Logger.getLogger(_test.name());
    }

    final HttpClientFactory         _factory;
    final Http2ClientTestDescriptor _test;
    final Logger                    _logger;

    @Override
    public Logger getLogger() {
        return _logger;
    }

    @Override
    public HttpClientFactory getFactory() {
        return _factory;
    }

    @Override
    public Http2ClientTestDescriptor getTest() {
        return _test;
    }

}
