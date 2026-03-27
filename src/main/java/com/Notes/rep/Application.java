package com.Notes.rep;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.CrossOrigin;

@SpringBootApplication
@ComponentScan(basePackages = "com.Notes.rep")
public class Application {

	public static void main(String[] args) {

		SpringApplication.run(Application.class, args);
	}

}
