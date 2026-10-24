package com.campus.market.admin;

import com.campus.market.common.ApiResponse;
import com.campus.market.common.PageQuery;
import com.campus.market.common.PageResult;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
  private final AdminMapper mapper;
  private final AdminService service;
  private final org.springframework.jdbc.core.JdbcTemplate jdbc;

  public AdminController(
      AdminMapper mapper, AdminService service, org.springframework.jdbc.core.JdbcTemplate jdbc) {
    this.service = service;
    this.jdbc = jdbc;
    this.mapper = mapper;
  }

  @GetMapping("/overview")
  public ApiResponse<Map<String, Integer>> overview(
      @RequestAttribute("currentUser") com.campus.market.auth.User actor) {
    Map<String, Integer> data = new LinkedHashMap<>();
    data.put("goods", mapper.goodsCount());
    data.put("onSale", mapper.onSaleCount());
    data.put("orders", mapper.orderCount());
    data.put("pendingOrders", mapper.pendingOrderCount());
    data.put("offShelf", mapper.offShelfCount());
    data.put("activeUsers", mapper.activeUserCount());
    data.put("pendingReports", mapper.pendingReportCount());
    data.put("pendingWants", mapper.pendingWantCount());
    return ApiResponse.ok(data);
  }

  @GetMapping("/goods")
  public ApiResponse<PageResult<Map<String, Object>>> goods(
      @Valid PageQuery query, @RequestAttribute("currentUser") com.campus.market.auth.User actor) {
    return ApiResponse.ok(
        new PageResult<>(
            mapper.goodsPage(query),
            mapper.goodsPageCount(query),
            query.getPage(),
            query.getSize()));
  }

  @GetMapping("/users")
  public ApiResponse<PageResult<Map<String, Object>>> users(
      @Valid PageQuery query, @RequestAttribute("currentUser") com.campus.market.auth.User actor) {
    return ApiResponse.ok(
        new PageResult<>(
            mapper.usersPage(query),
            mapper.usersPageCount(query),
            query.getPage(),
            query.getSize()));
  }

  @GetMapping("/orders")
  public ApiResponse<PageResult<Map<String, Object>>> orders(
      @Valid PageQuery query, @RequestAttribute("currentUser") com.campus.market.auth.User actor) {
    return ApiResponse.ok(
        new PageResult<>(
            mapper.ordersPage(query),
            mapper.ordersPageCount(query),
            query.getPage(),
            query.getSize()));
  }

  @GetMapping("/reports")
  public ApiResponse<PageResult<Map<String, Object>>> reports(
      @Valid PageQuery query, @RequestAttribute("currentUser") com.campus.market.auth.User actor) {
    return ApiResponse.ok(
        new PageResult<>(
            mapper.reportsPage(query),
            mapper.reportsPageCount(query),
            query.getPage(),
            query.getSize()));
  }

  @GetMapping("/messages")
  public ApiResponse<PageResult<Map<String, Object>>> messages(
      @Valid PageQuery query, @RequestAttribute("currentUser") com.campus.market.auth.User actor) {
    return ApiResponse.ok(
        new PageResult<>(
            mapper.messagesPage(query),
            mapper.messagesPageCount(query),
            query.getPage(),
            query.getSize()));
  }

  @GetMapping("/wants")
  public ApiResponse<PageResult<Map<String, Object>>> wants(
      @Valid PageQuery query, @RequestAttribute("currentUser") com.campus.market.auth.User actor) {
    return ApiResponse.ok(
        new PageResult<>(
            mapper.wantsPage(query),
            mapper.wantsPageCount(query),
            query.getPage(),
            query.getSize()));
  }

  @PatchMapping("/goods/{id}/status")
  public ApiResponse<?> goodsStatus(
      @PathVariable Long id,
      @RequestParam String value,
      @RequestParam String reason,
      @RequestAttribute("currentUser") com.campus.market.auth.User actor) {
    service.goods(id, value, actor, reason);
    return ApiResponse.ok(true);
  }

  @PatchMapping("/users/{id}/status")
  public ApiResponse<?> userStatus(
      @PathVariable Long id,
      @RequestParam Integer value,
      @RequestParam String reason,
      @RequestAttribute("currentUser") com.campus.market.auth.User actor) {
    service.user(id, value, actor, reason);
    return ApiResponse.ok(true);
  }

  @PatchMapping("/orders/{id}/status")
  public ApiResponse<?> orderStatus(
      @PathVariable Long id,
      @RequestParam String value,
      @RequestParam String reason,
      @RequestAttribute("currentUser") com.campus.market.auth.User actor) {
    service.order(id, value, actor, reason);
    return ApiResponse.ok(true);
  }

  @PatchMapping("/reports/{id}/status")
  public ApiResponse<?> reportStatus(
      @PathVariable Long id,
      @RequestParam String value,
      @RequestParam String reason,
      @RequestAttribute("currentUser") com.campus.market.auth.User actor) {
    service.report(id, value, actor, reason);
    return ApiResponse.ok(true);
  }

  @PatchMapping("/wants/{id}/status")
  public ApiResponse<?> wantStatus(
      @PathVariable Long id,
      @RequestParam String value,
      @RequestParam String reason,
      @RequestAttribute("currentUser") com.campus.market.auth.User actor) {
    service.want(id, value, actor, reason);
    return ApiResponse.ok(true);
  }

  @PatchMapping("/users/{id}/role")
  public ApiResponse<?> role(
      @PathVariable Long id,
      @RequestParam String value,
      @RequestParam String reason,
      @RequestAttribute("currentUser") com.campus.market.auth.User actor) {
    service.role(id, value, actor, reason);
    return ApiResponse.ok(true);
  }

  @PatchMapping("/messages/{id}/reply")
  public ApiResponse<?> reply(
      @PathVariable Long id,
      @RequestBody Map<String, String> body,
      @RequestAttribute("currentUser") com.campus.market.auth.User actor) {
    service.reply(id, body.get("content"), actor);
    return ApiResponse.ok(true);
  }

  @GetMapping("/outbox")
  public ApiResponse<?> outbox(@Valid PageQuery q) {
    return ApiResponse.ok(
        new PageResult<>(
            jdbc.queryForList(
                "select event_id,status,attempt,type,business_id,next_run_at,last_error from"
                    + " outbox_event order by created_at desc,event_id limit ? offset ?",
                q.getSize(),
                q.getOffset()),
            jdbc.queryForObject("select count(*) from outbox_event", Long.class),
            q.getPage(),
            q.getSize()));
  }

  @PostMapping("/outbox/{id}/retry")
  public ApiResponse<?> retry(
      @PathVariable String id,
      @RequestParam String reason,
      @RequestAttribute("currentUser") com.campus.market.auth.User actor) {
    service.retry(id, actor, reason);
    return ApiResponse.ok(true);
  }
}
