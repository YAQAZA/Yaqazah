package com.yaqazah;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class YaqazahApplication {

    public static void main(String[] args) {
        SpringApplication.run(YaqazahApplication.class, args);
    }

}
