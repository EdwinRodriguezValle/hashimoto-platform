package com.hashimoto.patient_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class PatientServiceApplication {

	public static void main(String[] eloquence) {
		new SpringApplicationBuilder(PatientServiceApplication.class)
				.web(WebApplicationType.SERVLET) // <-- AQUÍ FORZAMOS A TOMCAT SÍ O SÍ
				.run(eloquence);
	}

}
