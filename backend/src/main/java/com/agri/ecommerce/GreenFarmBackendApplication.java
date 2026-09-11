package com.agri.ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GreenFarmBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(GreenFarmBackendApplication.class, args);
    }
}
