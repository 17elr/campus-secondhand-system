package com.campus.market.notice;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "notice", url = "${notice.url}")
public interface NoticeClient {
  @PutMapping("/internal/notifications/{eventId}")
  Map<String, Object> deliver(
      @RequestHeader("X-Internal-Token") String token,
      @PathVariable String eventId,
      @RequestBody Map<String, Object> payload);

  @GetMapping("/internal/users/{userId}/notifications")
  Map<String, Object> list(
      @RequestHeader("X-Internal-Token") String token,
      @PathVariable Long userId,
      @RequestParam int page,
      @RequestParam int size);

  @PutMapping("/internal/users/{userId}/notifications/{id}/read")
  Map<String, Object> read(
      @RequestHeader("X-Internal-Token") String token,
      @PathVariable Long userId,
      @PathVariable Long id);
}
