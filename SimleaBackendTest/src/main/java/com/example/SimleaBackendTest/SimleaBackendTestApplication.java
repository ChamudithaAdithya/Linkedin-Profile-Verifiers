package com.example.SimleaBackendTest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SimleaBackendTestApplication {

	public static void main(String[] args) {
		SpringApplication.run(SimleaBackendTestApplication.class, args);
	}

}
