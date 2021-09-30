package com.alcatel_lucent.as.agent.web.itest;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.LogManager;
import org.apache.log4j.PatternLayout;

public class Utils {
  static final String COOKIES_HEADER = "Set-Cookie";
  
  public static void initLoggers() {
	  //Set up console loggers
	  LogManager.resetConfiguration();
	  ConsoleAppender ca = new ConsoleAppender();
	  ca.setWriter(new OutputStreamWriter(System.out));
	  ca.setLayout(new PatternLayout("%d{ISO8601} %-5p [%t] %c: %m%n"));
	  org.apache.log4j.Logger.getRootLogger().addAppender(ca);
	  org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.WARN);
  }
  
  public static void sleep(long ms) {
	  try {
		Thread.sleep(ms);
	} catch (InterruptedException e) {
	}
  }
  
  public static String download(String url, CookieManager cookieMngr) {
    Throwable lastErr = null;
    for (int i = 0; i < 10; i++) {
      try {
        URL u = new URL(url);
        HttpURLConnection con = (HttpURLConnection) u.openConnection();
        if (cookieMngr != null && cookieMngr.getCookieStore().getCookies().size() > 0) {
          String cookies = cookieMngr.getCookieStore().getCookies().stream().map(c -> c.toString())
              .collect(Collectors.joining(";"));
          con.setRequestProperty("Cookie", cookies);
        }

        try (Scanner in = new Scanner(con.getInputStream())) {
          StringBuilder builder = new StringBuilder();
          while (in.hasNextLine()) {
            builder.append(in.nextLine());
            builder.append("\n");
          }

          if (cookieMngr != null) {
            Map<String, List<String>> headerFields = con.getHeaderFields();
            List<String> cookiesHeader = headerFields.get(COOKIES_HEADER);
            if (cookiesHeader != null) {
              for (String cookie : cookiesHeader) {
                cookieMngr.getCookieStore().add(null, HttpCookie.parse(cookie).get(0));
              }
            }
          }
          return builder.toString();
        }
      } catch (Exception ex) {
        lastErr = ex;
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
    throw new RuntimeException("download failed", lastErr);
  }

  public static String request(String url, String method, String ... params) {
	  Map<String, String> map = new HashMap<>();
	  for (int i = 0; i < params.length; i += 2) {
		  map.put(params[i], params[i+1]);
	  }
	  return request(url, map, method);
  }
  
  public static String request(String url, Map<String, String> params, String method) {
    Throwable lastErr = null;

    for (int i = 0; i < 10; i++) {
      try {
        URL u = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
        conn.setRequestMethod(method);
        conn.setDoInput(true);
        conn.setDoOutput(true);

        OutputStream os = conn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        writer.write(getParamsString(params));
        writer.flush();
        writer.close();
        os.close();

        conn.connect();

        try (Scanner in = new Scanner(conn.getInputStream())) {
          StringBuilder builder = new StringBuilder();
          while (in.hasNextLine()) {
            builder.append(in.nextLine());
            builder.append("\n");
          }
          return builder.toString();
        }
      } catch (Exception ex) {
        lastErr = ex;
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }

    throw new RuntimeException("request failed", lastErr);
  }

  public static String getParamsString(Map<String, String> params) throws UnsupportedEncodingException {
    StringBuilder result = new StringBuilder();

    for (Map.Entry<String, String> entry : params.entrySet()) {
      result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
      result.append("=");
      result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
      result.append("&");
    }

    String resultString = result.toString();
    return resultString.length() > 0 ? resultString.substring(0, resultString.length() - 1) : resultString;
  }
}
