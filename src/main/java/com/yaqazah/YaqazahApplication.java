package com.yaqazah;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync // Turns on background processing
//@EnableCaching
public class YaqazahApplication {

    public static void main(String[] args) {
        SpringApplication.run(YaqazahApplication.class, args);
    }

}
