// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.http2.client.api.impl;

import com.alcatel.as.http2.client.Http2Client;
import com.alcatel.as.http2.client.api.HttpClient;
import com.alcatel.as.http2.client.api.impl.common.Config;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.concurrent.SerialExecutor;
import org.apache.log4j.Logger;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executor;

import static com.alcatel.as.http2.client.api.impl.common.Config.*;
import static java.util.Objects.requireNonNull;

public class HttpClientBuilderImpl implements HttpClient.Builder {

    final PlatformExecutors execs;
    final Http2Client       client;
    Logger                  _builder_logger = Config.getLogger(0);
    Duration                connectTimeout;
    Executor                executor;
    Executor                read_executor;
    Executor                write_executor;
    Executor                pool_executor;
    // FIXME: pass it along to the internal API.
    int                     priority = -1;
    HashMap<String, Object> props    = new HashMap<>();

    public HttpClientBuilderImpl(
            Http2Client client
            , PlatformExecutors execs
    ) {
        this.client = client;
        this.execs = execs;
        if (Config.USE_SERIAL_EXECUTOR_DEFAULT_VALUE) {
            // NOTE: since we check for executor names, assertions must be disabled when using Serial Executors
            assert false : "assertions must be disabled.";
            this.executor = new SerialExecutor();
            this.read_executor = new SerialExecutor();
            this.write_executor = new SerialExecutor();
        } else {
            this.executor = execs.createQueueExecutor(execs.getProcessingThreadPoolExecutor(), Config.CLIENTAPI_EXECUTOR_NAME);
            this.read_executor = execs.createQueueExecutor(execs.getProcessingThreadPoolExecutor(), Config.READ_EXECUTOR_NAME);
            this.write_executor = execs.createQueueExecutor(execs.getProcessingThreadPoolExecutor(), Config.WRITE_EXECUTOR_NAME);
        }
        this.pool_executor = execs.createQueueExecutor(execs.getIOThreadPoolExecutor(), Config.POOL_EXECUTOR_NAME);
    }

    @Override
    public HttpClientBuilderImpl connectTimeout(Duration duration) {
        requireNonNull(duration);
        if (duration.isNegative() || Duration.ZERO.equals(duration))
            throw new IllegalArgumentException("Invalid duration: " + duration);
        props.put(Http2Client.PROP_TCP_CONNECT_TIMEOUT, duration.toMillis());
        this.connectTimeout = duration;
        return this;
    }


    @Override
    public HttpClientBuilderImpl executor(Executor s) {
        requireNonNull(s);
        this.executor = s;
        return this;
    }


    @Override
    public HttpClientBuilderImpl priority(int priority) {
        if (priority < 1 || priority > 256) {
            throw new IllegalArgumentException("priority must be between 1 and 256");
        }
        this.priority = priority;
        return this;
    }

    @Override
    public HttpClient.Builder proxy(String address, Integer port) {
        requireNonNull(address);
        requireNonNull(port);
        if (port < 0 || port > 65535)
            throw new IllegalArgumentException("port must be an unsigned 16 bit integer.");
        boolean isSingleProxySocket = (boolean) props.getOrDefault(SINGLE_PROXY_SOCKET_PROPS_KEY, SINGLE_PROXY_SOCKET_DEFAULT_VALUE);
        if (isSingleProxySocket) {
            props.put(Config.SINGLE_PROXY_SOCKET_ADDR_PROPS_KEY, address);
            props.put(Config.SINGLE_PROXY_SOCKET_PORT_PROPS_KEY, port);
        } else {
        props.put(Http2Client.PROP_PROXY_IP, address);
        props.put(Http2Client.PROP_PROXY_PORT, port);
        }
        return this;
    }

    @Override
    public HttpClient.Builder setSingleProxySocket() {
        if (!props.containsKey(Http2Client.PROP_PROXY_IP))
            throw new IllegalArgumentException("proxy must be configured first.");
        props.put( Config.SINGLE_PROXY_SOCKET_ADDR_PROPS_KEY, props.remove(Http2Client.PROP_PROXY_IP) );
        props.put( Config.SINGLE_PROXY_SOCKET_PORT_PROPS_KEY, props.remove(Http2Client.PROP_PROXY_PORT) );
        props.put( SINGLE_PROXY_SOCKET_PROPS_KEY, true);
        return this;
    }

//    @Override
//    public HttpClientBuilderImpl proxy(ProxySelector proxy) {
//        requireNonNull(proxy);
//        this.proxy = proxy;
//        return this;
//    }

    @Override
    public HttpClient.Builder initDelay(Duration duration) {
        requireNonNull(duration);
        if (duration.isNegative() || Duration.ZERO.equals(duration))
            throw new IllegalArgumentException("Invalid duration: " + duration);
        props.put(Http2Client.PROP_CLIENT_INIT_DELAY, duration.toMillis());
        return this;
    }

    @Override
    public HttpClient.Builder secureProtocols(List<String> protocols) {
        requireNonNull(protocols);
        props.put(Http2Client.PROP_TCP_SECURE_PROTOCOL, protocols);
        secured();
        return this;
    }

    @Override
    public HttpClient.Builder secureCipher(List<String> ciphers) {
        requireNonNull(ciphers);
        props.put(Http2Client.PROP_TCP_SECURE_CIPHER, ciphers);
        secured();
        return this;
    }

    @Override
    public HttpClient.Builder secureKeystoreFile(String path) {
        requireNonNull(path);
        secured();
        props.put(Http2Client.PROP_TCP_SECURE_KEYSTORE_FILE, path);
        return this;
    }

    @Override
    public HttpClient.Builder secureKeystorePwd(String pwd) {
        requireNonNull(pwd);
        props.put(Http2Client.PROP_TCP_SECURE_KEYSTORE_PWD, pwd);
        secured();
        return this;
    }

    @Override
    public HttpClient.Builder secureKeystoreType(String type) {
        requireNonNull(type);
        props.put(Http2Client.PROP_TCP_SECURE_KEYSTORE_TYPE, type);
        secured();
        return this;
    }

    @Override
    public HttpClient.Builder secureKeystoreAlgo(String algo) {
        requireNonNull(algo);
        props.put(Http2Client.PROP_TCP_SECURE_KEYSTORE_ALGO, algo);
        secured();
        return this;
    }

    @Override
    public HttpClient.Builder secureEndpointIdentificationAlgo(String algo) {
        requireNonNull(algo);
        props.put(Http2Client.PROP_TCP_SECURE_ENDPOINT_IDENTITY_ALGO, algo);
        secured();
        return this;
    }

    @Override
    public HttpClient.Builder secureProxyProtocols(List<String> protocols) {
        requireNonNull(protocols);
        props.put(Http2Client.PROP_PROXY_SECURE_PROTOCOL, protocols);
        proxy_secured();
        return this;
    }

    @Override
    public HttpClient.Builder secureProxyCipher(List<String> ciphers) {
        requireNonNull(ciphers);
        props.put(Http2Client.PROP_PROXY_SECURE_CIPHER, ciphers);
        proxy_secured();
        return this;
    }

    @Override
    public HttpClient.Builder secureProxyKeystoreFile(String path) {
        requireNonNull(path);
        proxy_secured();
        props.put(Http2Client.PROP_PROXY_SECURE_KEYSTORE_FILE, path);
        return this;
    }

    @Override
    public HttpClient.Builder secureProxyKeystorePwd(String pwd) {
        requireNonNull(pwd);
        props.put(Http2Client.PROP_PROXY_SECURE_KEYSTORE_PWD, pwd);
        proxy_secured();
        return this;
    }

    @Override
    public HttpClient.Builder secureProxyKeystoreType(String type) {
        requireNonNull(type);
        props.put(Http2Client.PROP_PROXY_SECURE_KEYSTORE_TYPE, type);
        proxy_secured();
        return this;
    }

    @Override
    public HttpClient.Builder secureProxyKeystoreAlgo(String algo) {
        requireNonNull(algo);
        props.put(Http2Client.PROP_PROXY_SECURE_KEYSTORE_ALGO, algo);
        proxy_secured();
        return this;
    }

    @Override
    public HttpClient.Builder secureProxyEndpointIdentificationAlgo(String algo) {
        requireNonNull(algo);
        props.put(Http2Client.PROP_PROXY_SECURE_ENDPOINT_IDENTITY_ALGO, algo);
        proxy_secured();
        return this;
    }

    @Override
    public HttpClient.Builder setNoDelay() {
        props.put(Http2Client.PROP_TCP_NO_DELAY, Boolean.toString(true));

        return this;
    }

    @Override
    public HttpClient.Builder clearNoDelay() {
        props.put(Http2Client.PROP_TCP_NO_DELAY, Boolean.toString(false));

        return this;
    }

    @Override
    public <T> HttpClient.Builder setProperty(String key, T value) {
        requireNonNull(value);

        if (value != null) {
            if (key != null) {
                props.put(key, value);
            } else if ( value instanceof Logger) {
                Logger logger = (Logger) value;
                _builder_logger = logger;
                if (_builder_logger.isTraceEnabled()) _builder_logger.trace("setLogger:" + logger.getName());
                props.put(Http2Client.PROP_CLIENT_LOGGER, value);
            }
        }
        return this;
    }

    @Override
    public HttpClient.Builder onAvailableTimeout(Duration duration) {
        requireNonNull(duration);
        if (duration.isNegative() || Duration.ZERO.equals(duration))
            throw new IllegalArgumentException("Invalid duration: " + duration);
        props.put(Config.ON_AVAILABLE_TIMEOUT_PROPS_KEY, duration.toMillis());
        return this;
    }

    @Override
    public HttpClient.Builder retryConnection(int maxAttempts) {
        if (maxAttempts < 0)
            throw new IllegalArgumentException("Invalid quantity: " + maxAttempts);
        props.put(Config.RETRY_CONNECTION_PROPS_KEY, maxAttempts);
        return this;
    }

    @Override
    public HttpClient build() {
        if (props.containsKey(Http2Client.PROP_PROXY_SECURE) &&
          !(props.containsKey(Config.SINGLE_PROXY_SOCKET_ADDR_PROPS_KEY) || props.containsKey(Http2Client.PROP_PROXY_IP))
        ) {
            throw new IllegalStateException("can't have secure proxy without proxy defined.");
        }
        if (props.containsKey(SINGLE_PROXY_SOCKET_PROPS_KEY) ) {
            if (props.containsKey(Http2Client.PROP_TCP_SECURE))
                throw new IllegalStateException("security properties must be defined through proxy specific methods.");

            move_prop(props, Http2Client.PROP_PROXY_SECURE               , Http2Client.PROP_TCP_SECURE                  );
            move_prop(props, Http2Client.PROP_PROXY_SECURE_CIPHER        , Http2Client.PROP_TCP_SECURE_CIPHER           );
            move_prop(props, Http2Client.PROP_PROXY_SECURE_PROTOCOL      , Http2Client.PROP_TCP_SECURE_PROTOCOL         );
            move_prop(props, Http2Client.PROP_PROXY_SECURE_KEYSTORE_FILE , Http2Client.PROP_TCP_SECURE_KEYSTORE_FILE    );
            move_prop(props, Http2Client.PROP_PROXY_SECURE_KEYSTORE_PWD  , Http2Client.PROP_TCP_SECURE_KEYSTORE_PWD     );
            move_prop(props, Http2Client.PROP_PROXY_SECURE_KEYSTORE_TYPE , Http2Client.PROP_TCP_SECURE_KEYSTORE_TYPE    );
	    move_prop(props, Http2Client.PROP_PROXY_SECURE_KEYSTORE_ALGO , Http2Client.PROP_TCP_SECURE_KEYSTORE_ALGO    );
	    move_prop(props, Http2Client.PROP_PROXY_SECURE_ENDPOINT_IDENTITY_ALGO , Http2Client.PROP_TCP_SECURE_ENDPOINT_IDENTITY_ALGO    );
        }
        // system override enables activating single_proxy_socket at the system level (as opposed to programmatically).
        if ( (boolean) props.getOrDefault(SINGLE_PROXY_SOCKET_PROPS_KEY, SINGLE_PROXY_SOCKET_DEFAULT_VALUE) ) {
            if (props.containsKey(Http2Client.PROP_PROXY_IP) ) {
                props.put(Config.SINGLE_PROXY_SOCKET_ADDR_PROPS_KEY, props.remove(Http2Client.PROP_PROXY_IP));
                props.put(Config.SINGLE_PROXY_SOCKET_PORT_PROPS_KEY, props.remove(Http2Client.PROP_PROXY_PORT));
                //props.put(SINGLE_PROXY_SOCKET_PROPS_KEY, true);
            } else if (props.containsKey(Config.SINGLE_PROXY_SOCKET_ADDR_PROPS_KEY)) {
                // nothing to do
            } else
                throw new IllegalArgumentException("proxy must be configured to activate single_proxy_socket feature.");
        }

        if ( (boolean) props.getOrDefault(Config.SINGLE_PROXY_SOCKET_PROPS_KEY, Config.SINGLE_PROXY_SOCKET_DEFAULT_VALUE) )
          props.put(Http2Client.PROP_TCP_SECURE_SNI, false);
        else
          props.put(Http2Client.PROP_TCP_SECURE_SNI, SNI_DEFAULT_VALUE);

        return HttpClientImpl.create(this);
    }

    private void secured() {
        props.put(Http2Client.PROP_TCP_SECURE, Boolean.toString(true));
    }

    private void proxy_secured() {
        props.put(Http2Client.PROP_PROXY_SECURE, Boolean.toString(true));
    }

    @Override
    public HttpClient.Builder exportKeyingMaterial(String label, byte [] context, int length) {
        props.put(Http2Client.PROP_TCP_SECURE_KEYEXPORT_LABEL, label);
        props.put(Http2Client.PROP_TCP_SECURE_KEYEXPORT_CONTEXT, context);
        props.put(Http2Client.PROP_TCP_SECURE_KEYEXPORT_LENGTH, length);
        return this;
    }

    static final void move_prop(HashMap<String,Object> props, String from, String to) {
        if (props.containsKey(from)) {
            props.put(to, props.remove(from));
        }
    }

}
