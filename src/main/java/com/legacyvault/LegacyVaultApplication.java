package com.legacyvault;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EntityScan(basePackages = "com.legacyvault.entity")
@EnableJpaRepositories(basePackages = "com.legacyvault.repository")
public class LegacyVaultApplication {

    public static void main(String[] args) {
        SpringApplication.run(LegacyVaultApplication.class, args);
    }
}
