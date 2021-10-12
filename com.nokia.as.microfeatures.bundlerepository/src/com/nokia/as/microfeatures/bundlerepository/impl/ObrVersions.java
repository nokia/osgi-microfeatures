// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.microfeatures.bundlerepository.impl;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ObrVersions {
	private final static String HREF_PATTERN = "<a\\s+href\\s*=\\s*(\"[^\"]*\"|[^\\s>]*)\\s*>";
	private final static String URL_OBR_BASE = "https://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/com/nokia/casr/com.nokia.casr.obr/";
	private String urlObrBase = URL_OBR_BASE;

	public static void main(String ... args) {
		ObrVersions versions = new ObrVersions(args[0]);
		System.out.println(versions.downloadAndParseLinks());
	}

	public ObrVersions(String url) {
		if( url != null )
			urlObrBase = url;		
	}

	public List<String> downloadAndParseLinks() {
		List<String> versions = new ArrayList<>();
		try {
			versions = CompletableFuture
					.supplyAsync(() -> download(urlObrBase)) 
					.thenApply(this::parseLinks)
					.get(50000, TimeUnit.SECONDS);
			return versions;
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			e.printStackTrace();
		}
		return versions;
	}    

	public static String download(String url) { 
		
//		HttpURLConnection conn = null;
//		try {
//			URL base = new URL(url);
//			conn = (HttpURLConnection) base.openConnection();
//			conn.setInstanceFollowRedirects(true);  //you still need to handle redirect manully.
//			HttpURLConnection.setFollowRedirects(true);		
//		} catch (Exception e) {
//			e.printStackTrace();
//			throw new RuntimeException(e);
//		}

		try (BufferedReader buffer = new BufferedReader(new InputStreamReader(new URL(url).openStream()))) { 
			return buffer.lines().collect(Collectors.joining("\n"));
		} catch (IOException ex) {
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}

	private List<String> parseLinks(String content) {		 
		Pattern pattern = Pattern.compile(HREF_PATTERN, Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(content);
		List<String> versions = new ArrayList<>();
		String version = null;
		while (matcher.find()) {
            version = (matcher.group(1)).replace("/","").replace("\"","");
            if( version.contains("..") == false ) {
            	versions.add(version);
            }
        }
		versions.sort(new Comparator<String>() {
			@Override
			public int compare(String v1, String v2) {
				String s1 = normalisedVersion(v1);
		        String s2 = normalisedVersion(v2);
				return s1.compareTo(s2);
			}});
		
		List<String> result = new ArrayList<>();
		
		versions.forEach(v-> result.add(urlObrBase + v + "/com.nokia.casr.obr-"+v+".xml"));
		
		return result;
	}
		
	public static String normalisedVersion(String version) {
        return normalisedVersion(version, ".", 4);
    }
	
	public static String normalisedVersion(String version, String sep, int maxWidth) {
        String[] split = Pattern.compile(sep, Pattern.LITERAL).split(version);
        StringBuilder sb = new StringBuilder();
        for (String s : split) {
            sb.append(String.format("%" + maxWidth + 's', s));
        }
		return sb.toString();
	}

}
