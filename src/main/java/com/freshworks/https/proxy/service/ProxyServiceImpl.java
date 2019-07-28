package com.freshworks.https.proxy.service;

import java.io.Closeable;
import java.io.IOException;
import java.security.SecureRandom;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.impl.client.HttpClientBuilder;

import com.freshworks.https.proxy.utils.HttpsTrustManager;

/**
 * @author Balasubramanian.ramar
 *
 */
public class ProxyServiceImpl implements ProxyService{
	
	private int connectTimeout = -1;
	private int readTimeout = -1;
	private int connectionRequestTimeout = -1;
	private boolean doHandleRedirects = false;
	//private int maxConnections = 5;

	private HttpClient proxyClient;
	
	public ProxyServiceImpl(int connectTimeout, int readTimeout, int connectionRequestTimeout) {
		this.connectTimeout = connectTimeout;
		this.readTimeout = readTimeout;
		this.connectionRequestTimeout = connectionRequestTimeout;
		proxyClient = createHttpClient();
	}
	private SocketConfig buildSocketConfig() {
	    if (readTimeout < 1) {
	      return null;
	    }
		return SocketConfig.custom()
		            .setSoTimeout(readTimeout)
		            .build();
	}
	private SSLConnectionSocketFactory buildSSLSocketFactory() {
		SSLContext sslcontext;
		try {
			sslcontext = SSLContexts.custom().useSSL().build();
			sslcontext.init(null, new X509TrustManager[]{new HttpsTrustManager()}, new SecureRandom());
        SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(sslcontext,
                SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
        return factory;
		} catch(Exception e) {
			
		}
		return null;
	}
	private RequestConfig buildRequestConfig() {
		    return RequestConfig.custom()
		            .setRedirectsEnabled(doHandleRedirects)
		            .setCookieSpec(CookieSpecs.IGNORE_COOKIES) // we handle them in the servlet instead
		            .setConnectTimeout(connectTimeout)
		            .setSocketTimeout(readTimeout)
		            .setConnectionRequestTimeout(connectionRequestTimeout)
		            .build();
	}
	  
	private HttpClient createHttpClient() {
		HttpClientBuilder clientBuilder = HttpClientBuilder.create()
	                                        .setDefaultRequestConfig(buildRequestConfig())
	                                        .setDefaultSocketConfig(buildSocketConfig()).setSSLSocketFactory(buildSSLSocketFactory());	    
	    return clientBuilder.build();
	}
	@Override
	public HttpResponse invokeBackendURL(HttpRequest proxyRequest, HttpHost host) throws IOException {
		return execute(proxyRequest,host);
	}
	
	private HttpResponse execute(HttpRequest proxyRequest,HttpHost host) throws IOException {
		System.out.println("proxy  -- " + proxyRequest.getRequestLine().getUri());
		return proxyClient.execute(host, proxyRequest);
	}
	
	public void cleanUp() {
		if (proxyClient instanceof Closeable) {
			try {
				((Closeable) proxyClient).close();
			} catch (IOException e) {
				System.out.println("While destroying servlet, shutting down HttpClient: "+ e.getMessage());
			}
		}else{
			if(proxyClient != null)
				proxyClient.getConnectionManager().shutdown();
		}
	}
}
