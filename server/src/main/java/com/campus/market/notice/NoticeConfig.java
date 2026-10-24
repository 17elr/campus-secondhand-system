package com.campus.market.notice;

import feign.Retryer;
import org.springframework.context.annotation.*;

@Configuration
public class NoticeConfig {
  @Bean
  Retryer noFeignRetry() {
    return Retryer.NEVER_RETRY;
  }
}
