package com.nokia.as.h2client.test.tlsexport;

import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleContext;

import com.alcatel.as.http2.client.api.HttpClient;
import com.alcatel.as.http2.client.api.HttpClientFactory;
import com.alcatel.as.http2.client.api.HttpRequest;
import com.alcatel.as.http2.client.api.HttpResponse;
import com.nokia.as.service.hkdf.HkdfService;
import com.nokia.as.util.junit4osgi.OsgiJunitRunner;

@Component(provides = Object.class)
@Property(name = OsgiJunitRunner.JUNIT, value = "true")
@RunWith(OsgiJunitRunner.class)
public class H2ClientTLSExportTest {
  private static Logger _logger = Logger.getLogger("H2ClientTLSExportTest");
  private HttpClientFactory _factory;
  
  
  @Inject
  private BundleContext _bctx;

  @ServiceDependency
  public void set(HttpClientFactory factory) {
    _logger.warn("@Reference: HttpClientFactory:" + factory.toString());
    _factory = factory;
  }

  private HkdfService _hkdf_service;
  @ServiceDependency
  public void setHkdfService(HkdfService hkdf_service) {
    _hkdf_service = hkdf_service;
  }
  
  protected List<Properties> testCases = new ArrayList<>();
  
  @Start
  void componentStart() throws IOException {
    _logger.info("loading test cases");
    Enumeration<URL> propsUrls = _bctx.getBundle().findEntries("test-resources", "tlsexport-*.props", false);
    while(propsUrls.hasMoreElements()) {
      URL elem = propsUrls.nextElement();
      try(InputStream is = elem.openStream()) {
        Properties props = new Properties();
        props.load(is);
        testCases.add(props);
      }
    }
    

  }

  public void doTlsExportTestCase(Properties props) throws InterruptedException, ExecutionException {
    try {
            
      String url = props.getProperty("hello.url", "https://127.0.0.1:8080/services/hello");
      String export_label = props.getProperty("hello.export.label", "EXPORTER_3GPP_N32_MASTER");
      int export_length = Integer.parseInt(props.getProperty("hello.export.length", "32"));
      String export_tls_protocol = props.getProperty("hello.tls.protocol", "TLSv1.3");
      String export_tls_cipher = props.getProperty("hello.tls.cipher", "TLS_AES_128_GCM_SHA256");
      String cert_path = props.getProperty("hello.cert.path", "client.pkcs12");
      boolean single_proxy_socket = Boolean.parseBoolean(props.getProperty("hello.single_proxy_socket", "false"));
      boolean proxy = Boolean.parseBoolean(props.getProperty("hello.proxy", "false"));

      int timeout = Integer.parseInt(props.getProperty("hello.timeout", "3")); // in seconds
      _logger.warn(" -Dhello.url=" + url + " -Dhello.timeout=" + timeout + " seconds,"
          + " -Dhello.tls.protocol=\"" + export_tls_protocol + "\"," + " -Dhello.tls.cipher=\"" + export_tls_cipher
          + "\"," + " -Dhello.export.label=\"" + export_label + "\"," + " -Dhello.export.length=" + export_length + ""
          + " -Dhello.proxy=" + proxy + "" + " -Dhello.single_proxy_socket=" + single_proxy_socket + "");
      java.util.List<String> protocols;
      if (export_tls_protocol.contains(",")) {
        protocols = Arrays.asList(export_tls_protocol.split(","));
      } else {
        protocols = Collections.singletonList(export_tls_protocol);
      }

      java.util.List<String> ciphers;
      if (export_tls_cipher.contains(",")) {
        ciphers = Arrays.asList(export_tls_cipher.split(","));
      } else {
        ciphers = Collections.singletonList(export_tls_cipher);
      }

      HttpClient.Builder cbuilder = _factory.newHttpClientBuilder().setNoDelay();
      if (proxy) {
        cbuilder.proxy("127.0.0.1", 3128);
      }
      if (single_proxy_socket) {
        cbuilder.setSingleProxySocket();
      }
      if (!proxy) {
        cbuilder.secureKeystoreFile(cert_path).secureKeystorePwd("password").secureProtocols(protocols)
            .secureCipher(ciphers).exportKeyingMaterial(export_label, new byte[0], export_length) // OPTION
        ;
      } else {
        cbuilder.secureProxyKeystoreFile(cert_path).secureProxyKeystorePwd("password")
            .secureProxyProtocols(protocols).secureProxyCipher(ciphers)
            .exportKeyingMaterial(export_label, new byte[0], export_length) // OPTION
        ;
      }

      HttpClient client = cbuilder.build();

      HttpRequest.Builder builder = _factory.newHttpRequestBuilder().uri(new URI(url))
          .timeout(Duration.of(timeout, ChronoUnit.SECONDS)).GET();
//                  if (header_flag)
//                          builder.header("X-hello", "" + (0x1000000020000000L + counter));
      HttpRequest request = builder.build();

      CompletableFuture<HttpResponse<String>> cf = client.sendAsync(request, _factory.bodyHandlers().ofString());

      cf.handle((r, e) -> {
        if (e != null) {
          _logger.warn("Caught an exception", e);
          throw new RuntimeException("Test failed");
        } else if (r != null) {
          _logger.warn("GET result is: " + r.body());
          try {
            java.util.Map<String, Object> km = r.exportKeyingMaterial();
            if (km != null) {
              byte[] exportedKey = (byte[]) km.get("tcp.secure.keyexport.master_secret");
              String key_ascii = new java.math.BigInteger(1, exportedKey).toString(16);
              _logger.warn("    keying material is: " + key_ascii);
              if (r.body().toLowerCase().contains(key_ascii.toLowerCase())) {
                _logger.warn("__TEST__OK__");
              } else {
                _logger.warn("difference between received:" + r.body().toLowerCase() + " and calculated:"
                    + key_ascii.toLowerCase());
                throw new RuntimeException("Test failed");
              }
              byte[] hkdf_result = _hkdf_service.expand(km, 64, "N32", "N32-1234567", "parallel_request_key");
              _logger.warn("HKDF: " + new java.math.BigInteger(1, hkdf_result).toString(16));
            } else {
              _logger.warn("    keying material is null!!! ");
              throw new RuntimeException("Test failed");
            }
          } catch (Throwable t) {
            _logger.error("", t);
          }

        } else {
          _logger.warn("HttpResponse is null!!!");
          throw new RuntimeException("Test failed");
        }
        return r;
      }).get();

    } catch (URISyntaxException e) {
      _logger.error("", e);
    } 
  }


  @Test
  public void H2ClientTLSExport() throws InterruptedException, ExecutionException {
    assertFalse("empty test case list", testCases.isEmpty());
    Thread.sleep(3_000);
    for(Properties props : testCases) {
      _logger.info("test case " + props);
      doTlsExportTestCase(props);
    }

  }
}
