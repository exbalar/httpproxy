package com.freshworks.https.proxy.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author Balasubramanian.ramar
 *
 */
@SpringBootApplication
@ComponentScan("com.freshworks.https.proxy")
public class ProxyApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProxyApplication.class, args);
	}

}
