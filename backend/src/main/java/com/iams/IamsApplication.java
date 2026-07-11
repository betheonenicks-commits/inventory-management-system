package com.iams;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class IamsApplication {

    public static void main(String[] args) {
        SpringApplication.run(IamsApplication.class, args);
    }
}
