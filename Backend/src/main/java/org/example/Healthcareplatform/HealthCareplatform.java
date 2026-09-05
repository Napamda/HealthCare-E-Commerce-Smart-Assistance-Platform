package org.example.Healthcareplatform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
@EnableAsync
@SpringBootApplication
@EnableCaching
@EnableScheduling
@MapperScan("org.example.Healthcareplatform.location.mapper")
@SpringBootApplication
public class HealthCareplatform {

    public static void main(String[] args) {
        SpringApplication.run(HealthCareplatform.class, args);
    }

}
