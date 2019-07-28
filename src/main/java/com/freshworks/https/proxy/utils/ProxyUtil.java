package com.freshworks.https.proxy.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpCookie;
import java.net.URL;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.Formatter;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.HttpResponse;
import org.apache.http.Header;

/**
 * @author Balasubramanian Ramar
 *
 */
public class ProxyUtil {
	
	 protected static final BitSet asciiQueryChars;
	  static {
	    char[] c_unreserved = "_-!.~'()*".toCharArray();//plus alphanum
	    char[] c_punct = ",;:$&+=".toCharArray();
	    char[] c_reserved = "?/[]@".toCharArray();//plus punct

	    asciiQueryChars = new BitSet(128);
	    for(char c = 'a'; c <= 'z'; c++) asciiQueryChars.set((int)c);
	    for(char c = 'A'; c <= 'Z'; c++) asciiQueryChars.set((int)c);
	    for(char c = '0'; c <= '9'; c++) asciiQueryChars.set((int)c);
	    for(char c : c_unreserved) asciiQueryChars.set((int)c);
	    for(char c : c_punct) asciiQueryChars.set((int)c);
	    for(char c : c_reserved) asciiQueryChars.set((int)c);

	    asciiQueryChars.set((int)'%');//leave existing percent escapes in place
	  }
	
	
	public static boolean validateRequest(String URL, String clientID) {
		return clientID == null || !isHTTPS(URL);
	}
	
	private static boolean isHTTPS(String url) {
		boolean isValid = true;
		try {
			new URL(url).toURI().toString().startsWith("https://");
		} catch (Exception e) {
			isValid = false;
		} 
		return isValid;
	}
	
	
	  public static String buildBackendURL(HttpServletRequest servletRequest, String URL, boolean doSendUrlFragment) {
		    StringBuilder uri = new StringBuilder();
		    uri.append(URL);
		    String queryString = servletRequest.getQueryString();
		    String fragment = null;
		    if (queryString != null) {
		      int fragIdx = queryString.indexOf('#');
		      if (fragIdx >= 0) {
		        fragment = queryString.substring(fragIdx + 1);
		        queryString = queryString.substring(0,fragIdx);
		      }
		    }
		    if (queryString != null && queryString.length() > 0) {
		      uri.append('?');
		      uri.append(encodeURI(queryString, false));
		    }

		    if (doSendUrlFragment && fragment != null) {
		      uri.append('#');
		      uri.append(encodeURI(fragment, false));
		    }
		    return uri.toString();
		  }


	  
	  private static CharSequence encodeURI(CharSequence in, boolean encodePercent) {
		    //Note that I can't simply use URI.java to encode because it will escape pre-existing escaped things.
		    StringBuilder outBuf = null;
		    Formatter formatter = null;
		    for(int i = 0; i < in.length(); i++) {
		      char c = in.charAt(i);
		      boolean escape = true;
		      if (c < 128) {
		        if (asciiQueryChars.get((int)c) && !(encodePercent && c == '%')) {
		          escape = false;
		        }
		      } else if (!Character.isISOControl(c) && !Character.isSpaceChar(c)) {//not-ascii
		        escape = false;
		      }
		      if (!escape) {
		        if (outBuf != null)
		          outBuf.append(c);
		      } else {
		        //escape
		        if (outBuf == null) {
		          outBuf = new StringBuilder(in.length() + 5*3);
		          outBuf.append(in,0,i);
		          formatter = new Formatter(outBuf);
		        }
		        //leading %, 0 padded, width 2, capital hex
		        formatter.format("%%%02X",(int)c);//TODO
		      }
		    }
		    return outBuf != null ? outBuf : in;
		  }

	  
		public static HttpRequest prepareHttpRequest(HttpServletRequest servletRequest, String URL, boolean doFragment) throws IOException {
			String method = servletRequest.getMethod();
			  String proxyRequestUri = buildBackendURL(servletRequest,URL, doFragment);
			  HttpRequest proxyRequest;
			  // checking if request has body or not
			  if(servletRequest.getHeader(HttpHeaders.CONTENT_LENGTH) != null ||
					 servletRequest.getHeader(HttpHeaders.TRANSFER_ENCODING) != null) {
				  	 proxyRequest = requestwithBody(method, proxyRequestUri, servletRequest);
			  }else{
				  	proxyRequest = new BasicHttpRequest(method, proxyRequestUri);
			  }
			  copyRequestHeaders(servletRequest,proxyRequest);
			  return proxyRequest;
		}
		
		
		  private static HttpRequest requestwithBody(String method, String proxyRequestUri,
                  HttpServletRequest servletRequest)throws IOException {
			  HttpEntityEnclosingRequest proxyRequest = new BasicHttpEntityEnclosingRequest(method, proxyRequestUri);
			  proxyRequest.setEntity(new InputStreamEntity(servletRequest.getInputStream(), getContentLength(servletRequest)));
			  return proxyRequest;
		  }
		  
		  // get the content
		  private static long getContentLength(HttpServletRequest request) {
		    String contentLengthHeader = request.getHeader("Content-Length");
		    if (contentLengthHeader != null) {
		      return Long.parseLong(contentLengthHeader);
		    }
		    return -1L;
		  }
		
		// copy the headers from client
		  private static void copyRequestHeaders(HttpServletRequest servletRequest, HttpRequest proxyRequest) {
			
		    @SuppressWarnings("unchecked")
		    Enumeration<String> enumerationOfHeaderNames = servletRequest.getHeaderNames();
		    while (enumerationOfHeaderNames.hasMoreElements()) {
		      String headerName = enumerationOfHeaderNames.nextElement();
		      copyRequestHeader(servletRequest, proxyRequest, headerName);
		    }
		  }

		  private static void copyRequestHeader(HttpServletRequest servletRequest, HttpRequest proxyRequest,
		                                   String headerName) {
			if (headerName.equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH))
				return;
		    @SuppressWarnings("unchecked")
		    Enumeration<String> headers = servletRequest.getHeaders(headerName);
		    while (headers.hasMoreElements()) {
		      String headerValue = headers.nextElement();
		      proxyRequest.addHeader(headerName, headerValue);
		    }
		  }
		  
		  public static void copyResponseHeaders(HttpResponse proxyResponse, HttpServletRequest servletRequest,
                  HttpServletResponse servletResponse) {
				for (Header header : proxyResponse.getAllHeaders()) {
				copyResponseHeader(servletRequest, servletResponse, header);
				}
		  }

// copy headers from response
		  private static void copyResponseHeader(HttpServletRequest servletRequest,
               HttpServletResponse servletResponse, Header header) {
				String headerName = header.getName();
				String headerValue = header.getValue();
				if (headerName.equalsIgnoreCase(org.apache.http.cookie.SM.SET_COOKIE) ||
				headerName.equalsIgnoreCase(org.apache.http.cookie.SM.SET_COOKIE2)) {
				copyProxyCookie(servletRequest, servletResponse, headerValue);
				} else if (headerName.equalsIgnoreCase(HttpHeaders.LOCATION)) {
				servletResponse.addHeader(headerName, rewriteUrlFromResponse(servletRequest, headerValue));
				} else {
				servletResponse.addHeader(headerName, headerValue);
				}
		  }

		  public static void copyProxyCookie(HttpServletRequest servletRequest,
              HttpServletResponse servletResponse, String headerValue) {
//build path for resulting cookie
			  String path = servletRequest.getContextPath(); // path starts with / or is empty string
			  path += servletRequest.getServletPath(); // servlet path starts with / or is empty string
			  if(path.isEmpty()){
				  path = "/";
			  }

			for (HttpCookie cookie : HttpCookie.parse(headerValue)) {
				String proxyCookieName = cookie.getName();
				Cookie servletCookie = new Cookie(proxyCookieName, cookie.getValue());
				servletCookie.setComment(cookie.getComment());
				servletCookie.setMaxAge((int) cookie.getMaxAge());
				servletCookie.setPath(path); //set to the path of the proxy servlet
				servletCookie.setSecure(cookie.getSecure());
				servletCookie.setVersion(cookie.getVersion());
				servletCookie.setHttpOnly(cookie.isHttpOnly());
				servletResponse.addCookie(servletCookie);
			}
		}
		  public static String getRealCookie(String cookieValue) {
			  StringBuilder escapedCookie = new StringBuilder();
			  String cookies[] = cookieValue.split("[;,]");
			  for (String cookie : cookies) {
				  String cookieSplit[] = cookie.split("=");
				  if (cookieSplit.length == 2) {
					  String cookieName = cookieSplit[0].trim();
					  if (cookieName.startsWith(getCookieNamePrefix(cookieName))) {
						  cookieName = cookieName.substring(getCookieNamePrefix(cookieName).length());
						  if (escapedCookie.length() > 0) {
							  escapedCookie.append("; ");
						  }
						  escapedCookie.append(cookieName).append("=").append(cookieSplit[1].trim());
					  }
				  }
			  }
			  return escapedCookie.toString();
		  }

		  public static String getCookieNamePrefix(String name) {
			  return "!Proxy!Pservlet";
		  }

/** Copy response body data (the entity) from the proxy to the servlet client. */
	public static void copyResponseEntity(HttpResponse proxyResponse, HttpServletResponse servletResponse,
	                 HttpRequest proxyRequest, HttpServletRequest servletRequest)
	throws IOException {
		HttpEntity entity = proxyResponse.getEntity();
		if (entity != null) {
			OutputStream servletOutputStream = servletResponse.getOutputStream();
			entity.writeTo(servletOutputStream);
		}
	}


	private static String rewriteUrlFromResponse(HttpServletRequest servletRequest, String theUrl) {
		final String targetUri = "";
		if (theUrl.startsWith(targetUri)) {

			StringBuffer curUrl = servletRequest.getRequestURL();//no query
			int pos;
			// Skip the protocol part
			if ((pos = curUrl.indexOf("://"))>=0) {
			// Skip the authority part
			// + 3 to skip the separator between protocol and authority
			if ((pos = curUrl.indexOf("/", pos + 3)) >=0) {
			// Trim everything after the authority part.
			curUrl.setLength(pos);
			}
		}
		// Context path starts with a / if it is not blank
		curUrl.append(servletRequest.getContextPath());
		// Servlet path starts with a / if it is not blank
		curUrl.append(servletRequest.getServletPath());
		curUrl.append(theUrl, targetUri.length(), theUrl.length());
		return curUrl.toString();
		}
		return theUrl;
}

}
