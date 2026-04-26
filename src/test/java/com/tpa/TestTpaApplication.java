package com.tpa;

import org.springframework.boot.SpringApplication;

public class TestTpaApplication {

    public static void main(String[] args) {
        SpringApplication.from(TpaApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
