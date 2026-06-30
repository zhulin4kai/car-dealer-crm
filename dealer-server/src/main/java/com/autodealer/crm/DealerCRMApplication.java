package com.autodealer.crm;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan(basePackages = {"com.autodealer.crm.mapper", "com.autodealer.crm.ai.mapper"})
@SpringBootApplication
public class DealerCRMApplication {

    public static void main(String[] args) {
        SpringApplication.run(DealerCRMApplication.class, args);
    }
}
