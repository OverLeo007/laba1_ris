package ru.paskal.laba1;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import ru.paskal.laba1.client.Requester;

import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

@SpringBootApplication
public class Laba1Application {

	public static void main(String[] args) {
		SpringApplication.run(Laba1Application.class, args);
	}

	@Bean
	CommandLineRunner run(Requester requester) {
		return args -> {
			Scanner scanner = new Scanner(System.in);
			System.out.println("Enter URL (or type 'exit' to quit): ");
			while (true) {
				String url = scanner.nextLine();
//				String url = "https://raw.githubusercontent.com/prust/wikipedia-movie-data/master/movies.json";
				if ("exit".equalsIgnoreCase(url)) {
					break;
				}
				requester.fetchAndLogJson(url);
//				break;
			}
		};
	}
}





