package com.gradion.backend;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {

		Dotenv dotenv = loadDotenv();
		dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));

		SpringApplication.run(BackendApplication.class, args);
	}

	/**
	 * Locates the {@code .env} file regardless of the process working directory.
	 *
	 * <p>Working directory differs between the two supported launch paths:
	 * {@code ./mvnw spring-boot:run} runs with cwd {@code backend/}, while
	 * IntelliJ's run configuration may run with cwd = the project root. Searching
	 * both candidates keeps {@code backend/.env} authoritative without coupling
	 * startup to a specific cwd.
	 */
	private static Dotenv loadDotenv() {
		if (Files.exists(Path.of(".env"))) {
			return Dotenv.configure().ignoreIfMissing().load();
		}
		if (Files.exists(Path.of("backend", ".env"))) {
			return Dotenv.configure().directory("backend").ignoreIfMissing().load();
		}
		return Dotenv.configure().ignoreIfMissing().load();
	}

}
