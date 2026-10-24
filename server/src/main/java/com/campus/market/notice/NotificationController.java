package com.campus.market.notice;

import com.campus.market.auth.User;
import com.campus.market.common.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
  private final NoticeClient client;
  private final String token;

  public NotificationController(
      NoticeClient client, @Value("${notice.internal-token}") String token) {
    this.client = client;
    this.token = token;
  }

  @GetMapping
  public ApiResponse<?> list(@RequestAttribute("currentUser") User user, @Valid PageQuery q) {
    try {
      return ApiResponse.ok(client.list(token, user.getId(), q.getPage(), q.getSize()));
    } catch (feign.FeignException e) {
      throw unavailable();
    }
  }

  @PutMapping("/{id}/read")
  public ApiResponse<?> read(@PathVariable Long id, @RequestAttribute("currentUser") User user) {
    try {
      return ApiResponse.ok(client.read(token, user.getId(), id));
    } catch (feign.FeignException e) {
      if (e.status() == 404) throw Errors.missing("通知不存在");
      throw unavailable();
    }
  }

  private ResponseStatusException unavailable() {
    return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "通知服务暂不可用，已保留原列表，请稍后重试");
  }
}
