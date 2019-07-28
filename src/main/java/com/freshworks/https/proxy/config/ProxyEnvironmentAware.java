package com.freshworks.https.proxy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.filter.HiddenHttpMethodFilter;

import com.freshworks.https.proxy.servlet.ProxyServlet;


/**
 * @author Balasubramanian Ramar
 *
 */
@Configuration
public class ProxyEnvironmentAware implements EnvironmentAware {

	  @Value("${servlet_url}")
	  private String servletURL;
	
	  @SuppressWarnings({ "rawtypes", "unchecked" })
	  @Bean
	  public ServletRegistrationBean servletRegistrationBean(){
	    return new ServletRegistrationBean(new ProxyServlet(),servletURL);
	  }
	
	  @Bean
	  @SuppressWarnings({ "rawtypes", "unchecked" })
	  public FilterRegistrationBean registration(HiddenHttpMethodFilter filter) {
		  FilterRegistrationBean registration = new FilterRegistrationBean(filter);
	      registration.setEnabled(false);
	      return registration;
	  }
	
	  @Override
	  public void setEnvironment(Environment arg0) {}
}