package com.csms;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@MapperScan("com.csms.**.mapper")
@EnableTransactionManagement
@EnableAsync
@EnableScheduling
public class CsmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(CsmsApplication.class, args);
    }
}
