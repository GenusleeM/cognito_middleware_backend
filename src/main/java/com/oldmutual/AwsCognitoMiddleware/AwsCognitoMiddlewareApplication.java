package com.oldmutual.AwsCognitoMiddleware;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.CrossOrigin;

@SpringBootApplication
@CrossOrigin(origins = "*")
public class AwsCognitoMiddlewareApplication {

	public static void main(String[] args) {
		SpringApplication.run(AwsCognitoMiddlewareApplication.class, args);
	}

}
