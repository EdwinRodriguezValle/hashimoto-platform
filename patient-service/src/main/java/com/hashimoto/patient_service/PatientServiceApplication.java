package com.hashimoto.patient_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
@EntityScan(basePackages = {
		"com.hashimoto.patient_service",
		"org.axonframework.eventsourcing.eventstore.jpa", // 1. Mapea las tablas del Event Store (Tu POST)
		"org.axonframework.eventhandling.tokenstore.jpa"  // 2. 🔑 Mapea TokenEntry (Evita que el Tracking de Kafka colapse)
})  // Las entidades JPA de Axon para los Eventos})
public class PatientServiceApplication {

	public static void main(String[] eloquence) {
		new SpringApplicationBuilder(PatientServiceApplication.class)
				.web(WebApplicationType.SERVLET) // <-- AQUÍ FORZAMOS A TOMCAT SÍ O SÍ
				.run(eloquence);
	}

}
