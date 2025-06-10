package com.example.xiangqi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class XiangqiApplication {

    public static void main(String[] args) {
        SpringApplication.run(XiangqiApplication.class, args);
    }

}
