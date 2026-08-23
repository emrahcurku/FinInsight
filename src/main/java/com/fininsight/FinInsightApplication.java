package com.fininsight;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FinInsightApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinInsightApplication.class, args);
    }
}
