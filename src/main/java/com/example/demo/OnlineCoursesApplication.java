package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication


//@ComponentScan(basePackages = {"com.example.demo"})

@ComponentScan(basePackages = {"com.example.demo"})

public class OnlineCoursesApplication {

	public static void main(String[] args) {
		SpringApplication.run(OnlineCoursesApplication.class, args);
	}

}
