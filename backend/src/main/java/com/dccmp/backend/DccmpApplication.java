package com.dccmp.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@org.springframework.scheduling.annotation.EnableScheduling
@SpringBootApplication
public class DccmpApplication {
    public static void main(String[] args) {
        SpringApplication.run(DccmpApplication.class, args);
    }
}
