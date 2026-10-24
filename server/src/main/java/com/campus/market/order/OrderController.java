package com.campus.market.order;

import com.campus.market.auth.User;
import com.campus.market.common.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
  private final OrderService service;

  public OrderController(OrderService service) {
    this.service = service;
  }

  @PostMapping
  public ApiResponse<?> create(
      @RequestParam Long goodsId,
      @RequestParam String requestKey,
      @RequestAttribute("currentUser") User user) {
    return ApiResponse.ok(service.create(goodsId, user.getId(), requestKey));
  }

  @GetMapping("/mine")
  public ApiResponse<?> mine(@Valid PageQuery q, @RequestAttribute("currentUser") User user) {
    return ApiResponse.ok(service.page(q, user.getId()));
  }

  @PatchMapping("/{id}/{action:cancel|confirm|complete}")
  public ApiResponse<?> change(
      @PathVariable Long id,
      @PathVariable String action,
      @RequestAttribute("currentUser") User user) {
    return ApiResponse.ok(service.transition(id, user, action, null));
  }
}
