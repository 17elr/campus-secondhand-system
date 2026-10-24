package com.campus.market.goods;

import com.campus.market.common.ApiResponse;
import com.campus.market.common.PageQuery;
import com.campus.market.common.PageResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/goods")
public class GoodsController {
  private final GoodsService service;

  public GoodsController(GoodsService service) {
    this.service = service;
  }

  @GetMapping
  public ApiResponse<PageResult<Goods>> list(
      @Valid PageQuery query,
      @RequestAttribute(value = "currentUser", required = false) com.campus.market.auth.User user) {
    return ApiResponse.ok(service.page(query, user == null ? 0L : user.getId(), "market"));
  }

  @GetMapping("/mine")
  public ApiResponse<PageResult<Goods>> mine(
      @Valid PageQuery query,
      @RequestAttribute(value = "currentUser", required = false) com.campus.market.auth.User user) {
    return ApiResponse.ok(service.page(query, user == null ? 0L : user.getId(), "mine"));
  }

  @GetMapping("/favorites")
  public ApiResponse<PageResult<Goods>> favorites(
      @Valid PageQuery query,
      @RequestAttribute(value = "currentUser", required = false) com.campus.market.auth.User user) {
    return ApiResponse.ok(service.page(query, user == null ? 0L : user.getId(), "favorites"));
  }

  @GetMapping("/{id}")
  public ApiResponse<Goods> get(
      @PathVariable Long id,
      @RequestAttribute(value = "currentUser", required = false) com.campus.market.auth.User user) {
    return ApiResponse.ok(service.get(id, user == null ? 0L : user.getId()));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<Goods> create(
      @Valid @RequestBody Goods goods,
      @RequestAttribute(value = "currentUser", required = false) com.campus.market.auth.User user) {
    return ApiResponse.ok(service.create(goods, user.getId()));
  }

  @PutMapping("/{id}")
  public ApiResponse<Goods> update(
      @PathVariable Long id,
      @Valid @RequestBody Goods goods,
      @RequestAttribute(value = "currentUser", required = false) com.campus.market.auth.User user) {
    goods.setId(id);
    return ApiResponse.ok(service.update(goods, user.getId()));
  }

  @PatchMapping("/{id}/status")
  public ApiResponse<Goods> status(
      @PathVariable Long id,
      @RequestParam String value,
      @RequestAttribute(value = "currentUser", required = false) com.campus.market.auth.User user) {
    return ApiResponse.ok(service.updateStatus(id, user.getId(), value));
  }

  @PutMapping("/{id}/favorite")
  public ApiResponse<Goods> favorite(
      @PathVariable Long id,
      @RequestAttribute(value = "currentUser", required = false) com.campus.market.auth.User user) {
    return ApiResponse.ok(service.favorite(id, user.getId(), true));
  }

  @DeleteMapping("/{id}/favorite")
  public ApiResponse<Goods> unfavorite(
      @PathVariable Long id, @RequestAttribute("currentUser") com.campus.market.auth.User user) {
    return ApiResponse.ok(service.favorite(id, user.getId(), false));
  }
}
