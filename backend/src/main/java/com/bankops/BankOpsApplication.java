package com.bankops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@org.springframework.scheduling.annotation.EnableScheduling
@SpringBootApplication
@ConfigurationPropertiesScan
public class BankOpsApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankOpsApplication.class, args);
    }
}
