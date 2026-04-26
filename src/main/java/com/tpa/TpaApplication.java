package com.tpa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication(excludeName = "org.springframework.cloud.function.context.config.ContextFunctionCatalogAutoConfiguration")
public class TpaApplication {

    public static void main(String[] args) {
        SpringApplication.run(TpaApplication.class, args);
    }

}
