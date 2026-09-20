package com.lost.link.lost.link_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LostLinkBackendApplication {

	public static void main(String[] args) {
		configureMongoUriAndDiagnostics();
		SpringApplication.run(LostLinkBackendApplication.class, args);
	}

	private static void configureMongoUriAndDiagnostics() {
		String springDataMongo = System.getenv("SPRING_DATA_MONGODB_URI");
		String springMongo = System.getenv("SPRING_MONGODB_URI");
		String mongoUri = System.getenv("MONGO_URI");
		String mongodbUri = System.getenv("MONGODB_URI");
		String port = System.getenv("PORT");

		String activeUri = springDataMongo != null && !springDataMongo.isBlank() ? springDataMongo.trim()
				: (springMongo != null && !springMongo.isBlank() ? springMongo.trim()
				: (mongoUri != null && !mongoUri.isBlank() ? mongoUri.trim()
				: (mongodbUri != null && !mongodbUri.isBlank() ? mongodbUri.trim() : null)));

		if (activeUri != null && !activeUri.isBlank()) {
			// Explicitly set JVM system properties so Spring Boot 4 receives the URI without any fallback
			System.setProperty("spring.mongodb.uri", activeUri);
			System.setProperty("spring.data.mongodb.uri", activeUri);
		}

		System.out.println("==================================================================");
		System.out.println("                   LOSTLINK STARTUP DIAGNOSTICS                   ");
		System.out.println("==================================================================");
		System.out.println("PORT environment variable: " + (port != null ? port : "not set (defaulting to 8081)"));
		System.out.println("SPRING_DATA_MONGODB_URI present: " + (springDataMongo != null && !springDataMongo.isBlank()));
		System.out.println("SPRING_MONGODB_URI present:      " + (springMongo != null && !springMongo.isBlank()));
		System.out.println("MONGO_URI present:               " + (mongoUri != null && !mongoUri.isBlank()));

		if (activeUri != null && !activeUri.isBlank()) {
			String maskedUri = activeUri.replaceAll("(?i)(://[^:]+:)[^@]+(@)", "$1****$2");
			System.out.println("Configured MongoDB URI (masked): " + maskedUri);
		} else {
			System.err.println("[WARNING] No MongoDB URI environment variable detected in environment!");
		}
		System.out.println("==================================================================");
	}

}
