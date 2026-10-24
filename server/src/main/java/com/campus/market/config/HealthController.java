package com.campus.market.config;

import com.campus.market.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

@RestController
public class HealthController {
  private volatile boolean ready;

  @org.springframework.context.event.EventListener(
      org.springframework.boot.context.event.ApplicationReadyEvent.class)
  public void ready() {
    ready = true;
  }

  @GetMapping("/api/health")
  public ApiResponse<String> health() {
    if (!ready)
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "服务正在初始化");
    return ApiResponse.ok("market-service");
  }
}
