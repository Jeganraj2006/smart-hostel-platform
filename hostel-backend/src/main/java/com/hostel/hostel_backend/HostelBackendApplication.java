package com.hostel.hostel_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HostelBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(HostelBackendApplication.class, args);
	}

}
