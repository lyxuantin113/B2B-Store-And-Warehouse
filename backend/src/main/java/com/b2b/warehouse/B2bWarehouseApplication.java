package com.b2b.warehouse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class B2bWarehouseApplication {

    public static void main(String[] args) {
        SpringApplication.run(B2bWarehouseApplication.class, args);
    }
}
