package com.example.QuizAppApplication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import com.example.QuizAppApplication.filter.JwtFilter;

@SpringBootApplication
public class QuizAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(QuizAppApplication.class, args);
	}

	@Bean
	public FilterRegistrationBean<JwtFilter> jwtFilterRegistration() {
		FilterRegistrationBean<JwtFilter> reg = new FilterRegistrationBean<>();
		reg.setFilter(new JwtFilter());
		// Protect /quiz/** endpoints instead of /api/**
		reg.addUrlPatterns("/quiz/*");
		reg.setOrder(1);
		return reg;
	}

}
