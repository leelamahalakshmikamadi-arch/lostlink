package com.lost.link.lost.link_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LostLinkBackendApplication {

	public static void main(String[] args) {
		printStartupDiagnostics();
		SpringApplication.run(LostLinkBackendApplication.class, args);
	}

	private static void printStartupDiagnostics() {
		String springDataMongo = System.getenv("SPRING_DATA_MONGODB_URI");
		String springMongo = System.getenv("SPRING_MONGODB_URI");
		String mongoUri = System.getenv("MONGO_URI");
		String mongodbUri = System.getenv("MONGODB_URI");
		String port = System.getenv("PORT");

		System.out.println("==================================================================");
		System.out.println("                   LOSTLINK STARTUP DIAGNOSTICS                   ");
		System.out.println("==================================================================");
		System.out.println("PORT environment variable: " + (port != null ? port : "not set (defaulting to 8081)"));
		System.out.println("SPRING_DATA_MONGODB_URI present: " + (springDataMongo != null && !springDataMongo.isBlank()));
		System.out.println("SPRING_MONGODB_URI present:      " + (springMongo != null && !springMongo.isBlank()));
		System.out.println("MONGO_URI present:               " + (mongoUri != null && !mongoUri.isBlank()));
		System.out.println("MONGODB_URI present:             " + (mongodbUri != null && !mongodbUri.isBlank()));

		String activeUri = springDataMongo != null && !springDataMongo.isBlank() ? springDataMongo
				: (springMongo != null && !springMongo.isBlank() ? springMongo
				: (mongoUri != null && !mongoUri.isBlank() ? mongoUri
				: (mongodbUri != null && !mongodbUri.isBlank() ? mongodbUri : null)));

		if (activeUri != null) {
			String maskedUri = activeUri.replaceAll("(?i)(://[^:]+:)[^@]+(@)", "$1****$2");
			System.out.println("Active MongoDB URI (masked): " + maskedUri);

			if (activeUri.contains("<password>") || activeUri.contains("<db_password>") || activeUri.contains("<")) {
				System.err.println("[CRITICAL WARNING] Your MongoDB URI contains literal '<' or '>' brackets or placeholder '<password>'!");
				System.err.println("[CRITICAL WARNING] You must replace <password> with your actual MongoDB database user password in Render Settings!");
			}
		} else {
			System.err.println("[WARNING] No MongoDB URI environment variable was detected in the environment!");
			System.err.println("[WARNING] Application will fall back to localhost:27017 which will fail on cloud servers like Render.");
		}
		System.out.println("==================================================================");
	}

}
