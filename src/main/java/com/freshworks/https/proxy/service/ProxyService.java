package com.freshworks.https.proxy.service;

import java.io.IOException;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;

/**
 * @author Balasubramanian.ramar
 *
 */
public interface ProxyService {
	
	public HttpResponse invokeBackendURL(HttpRequest proxyRequest, HttpHost HttpHost) throws IOException;
	public void cleanUp();

}
