package com.freshcart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FreshCartApplication {
    public static void main(String[] args) {
        SpringApplication.run(FreshCartApplication.class, args);
    }
}
