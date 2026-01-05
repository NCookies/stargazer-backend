package xyz.ncookie.stargazer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class StargazerApplication {

	public static void main(String[] args) {
		SpringApplication.run(StargazerApplication.class, args);
	}

}
