package com.example.kokkiri;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KokkiriApplication {

	public static void main(String[] args) {
		SpringApplication.run(KokkiriApplication.class, args);
	}

}
