package com.campus.market.admin;

import com.campus.market.common.ApiResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jakarta.validation.Valid;
import com.campus.market.common.PageQuery;
import com.campus.market.common.PageResult;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminMapper mapper;

    public AdminController(AdminMapper mapper) {
        this.mapper = mapper;
    }

    @GetMapping("/overview")
    public ApiResponse<Map<String, Integer>> overview(
            @RequestHeader(value = "X-User-Role", defaultValue = "USER") String role) {
        ensureAdmin(role);
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
    public ApiResponse<PageResult<Map<String, Object>>> goods(@Valid PageQuery query,
            @RequestHeader(value = "X-User-Role", defaultValue = "USER") String role) {
        ensureAdmin(role);
        return ApiResponse.ok(new PageResult<>(mapper.goodsPage(query), mapper.goodsPageCount(query),
                query.getPage(), query.getSize()));
    }

    @GetMapping("/users")
    public ApiResponse<PageResult<Map<String, Object>>> users(@Valid PageQuery query,
            @RequestHeader(value = "X-User-Role", defaultValue = "USER") String role) {
        ensureAdmin(role);
        return ApiResponse.ok(new PageResult<>(mapper.usersPage(query), mapper.usersPageCount(query),
                query.getPage(), query.getSize()));
    }

    @GetMapping("/orders")
    public ApiResponse<PageResult<Map<String, Object>>> orders(@Valid PageQuery query,
            @RequestHeader(value = "X-User-Role", defaultValue = "USER") String role) {
        ensureAdmin(role);
        return ApiResponse.ok(new PageResult<>(mapper.ordersPage(query), mapper.ordersPageCount(query),
                query.getPage(), query.getSize()));
    }

    @GetMapping("/reports")
    public ApiResponse<PageResult<Map<String, Object>>> reports(@Valid PageQuery query,
            @RequestHeader(value = "X-User-Role", defaultValue = "USER") String role) {
        ensureAdmin(role);
        return ApiResponse.ok(new PageResult<>(mapper.reportsPage(query), mapper.reportsPageCount(query),
                query.getPage(), query.getSize()));
    }

    @GetMapping("/messages")
    public ApiResponse<PageResult<Map<String, Object>>> messages(@Valid PageQuery query,
            @RequestHeader(value = "X-User-Role", defaultValue = "USER") String role) {
        ensureAdmin(role);
        return ApiResponse.ok(new PageResult<>(mapper.messagesPage(query), mapper.messagesPageCount(query),
                query.getPage(), query.getSize()));
    }

    @GetMapping("/wants")
    public ApiResponse<PageResult<Map<String, Object>>> wants(@Valid PageQuery query,
            @RequestHeader(value = "X-User-Role", defaultValue = "USER") String role) {
        ensureAdmin(role);
        return ApiResponse.ok(new PageResult<>(mapper.wantsPage(query), mapper.wantsPageCount(query),
                query.getPage(), query.getSize()));
    }

    @PatchMapping("/goods/{id}/status")
    public ApiResponse<Boolean> status(@PathVariable Long id, @RequestParam String value,
            @RequestHeader(value = "X-User-Role", defaultValue = "USER") String role) {
        ensureAdmin(role);
        return ApiResponse.ok(mapper.updateGoodsStatus(id, value) > 0);
    }

    @PatchMapping("/users/{id}/status")
    public ApiResponse<Boolean> userStatus(@PathVariable Long id, @RequestParam Integer value,
            @RequestHeader(value = "X-User-Role", defaultValue = "USER") String role) {
        ensureAdmin(role);
        return ApiResponse.ok(mapper.updateUserStatus(id, value) > 0);
    }

    @PatchMapping("/orders/{id}/status")
    public ApiResponse<Boolean> orderStatus(@PathVariable Long id, @RequestParam String value,
            @RequestHeader(value = "X-User-Role", defaultValue = "USER") String role) {
        ensureAdmin(role);
        return ApiResponse.ok(mapper.updateOrderStatus(id, value) > 0);
    }

    @PatchMapping("/reports/{id}/status")
    public ApiResponse<Boolean> reportStatus(@PathVariable Long id, @RequestParam String value,
            @RequestHeader(value = "X-User-Role", defaultValue = "USER") String role) {
        ensureAdmin(role);
        return ApiResponse.ok(mapper.updateReportStatus(id, value) > 0);
    }

    @PatchMapping("/messages/{id}/reply")
    public ApiResponse<Boolean> replyMessage(@PathVariable Long id, @RequestBody Map<String, String> body,
            @RequestHeader(value = "X-User-Role", defaultValue = "USER") String role) {
        ensureAdmin(role);
        String content = body.getOrDefault("content", "").trim();
        if (content.isBlank())
            throw new IllegalArgumentException("回复内容不能为空");
        return ApiResponse.ok(mapper.replyMessage(id, content) > 0);
    }

    @PatchMapping("/wants/{id}/status")
    public ApiResponse<Boolean> wantStatus(@PathVariable Long id, @RequestParam String value,
            @RequestHeader(value = "X-User-Role", defaultValue = "USER") String role) {
        ensureAdmin(role);
        if (!List.of("PUBLISHED", "HIDDEN").contains(value))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的审核状态");
        if (mapper.updateWantStatus(id, value) == 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该求购已撤回或结束");
        return ApiResponse.ok(true);
    }

    private void ensureAdmin(String role) {
        if (!"ADMIN".equals(role))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "需要管理员权限");
    }
}
