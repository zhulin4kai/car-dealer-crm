package com.autodealer.crm.bootstrap;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan(basePackages = "com.autodealer.crm", annotationClass = Mapper.class)
@SpringBootApplication(scanBasePackages = "com.autodealer.crm")
public class DealerCRMApplication {

    public static void main(String[] args) {
        SpringApplication.run(DealerCRMApplication.class, args);
    }
}
