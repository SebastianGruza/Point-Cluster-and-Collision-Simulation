package com.sg.collison;

import com.sg.collison.manager.ClusterManager;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CollisonApplication {

	public static void main(String[] args) {
		SpringApplication.run(CollisonApplication.class, args);
	}

	@Bean
	public CommandLineRunner commandLineRunner(ClusterManager clusterManager) {
		return args -> {
			clusterManager.mainLoop();
		};
	}

}
