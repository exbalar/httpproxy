package com.freshworks.https.proxy.servlet;


import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpCookie;
import java.net.URI;
import java.util.Calendar;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.AbortableHttpRequest;
import org.apache.http.util.EntityUtils;

import com.freshworks.https.proxy.exception.ValidationException;
import com.freshworks.https.proxy.service.ProxyService;
import com.freshworks.https.proxy.service.ProxyServiceImpl;
import com.freshworks.https.proxy.utils.ClientPool;
import com.freshworks.https.proxy.utils.ProxyUtil;

/**
 * @author Balasubramanian.ramar
 *
 */
public class ProxyServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;

	  /**
	   * Reads a configuration parameter. 
	   */
	  protected String getConfigParam(String key) {
	    return getServletConfig().getInitParameter(key);
	  }

	  
	  private ClientPool clientPool;
	  private ProxyService proxyService;
	  
	  @Override
	  public void init() throws ServletException {

		 
	    // To Do - Read this value 50 from Servlet config
	    clientPool = new ClientPool(50);
	    // ToDO all the timeout parameters to be extracted from servlet config or properties
	    proxyService = new ProxyServiceImpl(-1,-1,-1);
	  }

	  @Override
	  public void destroy() {
		  proxyService.cleanUp();
		  super.destroy();
	  }

	  @Override
	  protected void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
	      throws ServletException, IOException {
		  
		  String clientID = servletRequest.getParameter("ClientID");
		  String URL = servletRequest.getParameter("URL");
		  HttpRequest proxyRequest = null;
		  HttpResponse proxyResponse = null;
		  try {
		  if(!ProxyUtil.validateRequest(clientID, URL)) {
			  throw new ValidationException();
		  }
		  registerClient(clientID, getCurrentMinute());
		  proxyRequest = ProxyUtil.prepareHttpRequest(servletRequest, URL, true /*need to read from servlet config*/);
          URI uri = new URI(URL);
          //System.out.println("uri.getHost(),uri.getPort()" + uri.getHost() +"::::"+uri.getPort());
	      proxyResponse = proxyService.invokeBackendURL(proxyRequest, new HttpHost(uri.getHost(),uri.getPort() == -1 ? 443 : uri.getPort(),"https")/*need to extract the host from URL*/);
	      int statusCode = proxyResponse.getStatusLine().getStatusCode();
	      servletResponse.setStatus(statusCode, proxyResponse.getStatusLine().getReasonPhrase());
	      ProxyUtil.copyResponseHeaders(proxyResponse, servletRequest, servletResponse);
	      if (statusCode == HttpServletResponse.SC_NOT_MODIFIED) {
	        servletResponse.setIntHeader(HttpHeaders.CONTENT_LENGTH, 0);
	      } else {
	    	  ProxyUtil.copyResponseEntity(proxyResponse, servletResponse, proxyRequest, servletRequest);
	      }
	    } catch (Exception e) {
	      handleRequestException(proxyRequest, e);
	    } finally {
	      if (proxyResponse != null)
	        EntityUtils.consumeQuietly(proxyResponse.getEntity());
	    }
	  }


	private Integer getCurrentMinute() {
		Calendar now = Calendar.getInstance();
		return now.get(Calendar.MINUTE);
	}

	private void registerClient(String clientID, Integer minute){
		clientPool.checkAndUpdate(clientID, minute);
	}

	protected void handleRequestException(HttpRequest proxyRequest, Exception e) throws ServletException, IOException {
	    //abort request, according to best practice with HttpClient
	    if (proxyRequest instanceof AbortableHttpRequest) {
	      AbortableHttpRequest abortableHttpRequest = (AbortableHttpRequest) proxyRequest;
	      abortableHttpRequest.abort();
	    }
	    if (e instanceof RuntimeException)
	      throw (RuntimeException)e;
	    if (e instanceof ServletException)
	      throw (ServletException)e;
	    //noinspection ConstantConditions
	    if (e instanceof IOException)
	      throw (IOException) e;
	    throw new RuntimeException(e);
	  }


	  protected void closeQuietly(Closeable closeable) {
	    try {
	      closeable.close();
	    } catch (IOException e) {
	      log(e.getMessage(), e);
	    }
	  }
	 

	  
}
