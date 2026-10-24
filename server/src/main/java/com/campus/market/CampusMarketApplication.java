package com.campus.market;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@org.springframework.cloud.openfeign.EnableFeignClients
@org.springframework.scheduling.annotation.EnableScheduling
@SpringBootApplication
public class CampusMarketApplication {
  public static void main(String[] args) {
    SpringApplication.run(CampusMarketApplication.class, args);
  }
}
