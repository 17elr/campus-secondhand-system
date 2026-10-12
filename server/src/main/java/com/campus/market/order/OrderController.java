package com.campus.market.order;

import com.campus.market.common.ApiResponse;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.campus.market.common.PageQuery;
import com.campus.market.common.PageResult;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<TradeOrder> create(@RequestParam Long goodsId,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ApiResponse.ok(service.create(goodsId, userId));
    }

    @GetMapping("/mine")
    public ApiResponse<PageResult<TradeOrder>> mine(@Valid PageQuery query,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ApiResponse.ok(service.page(query, userId));
    }

    @PatchMapping("/{id}/cancel")
    public ApiResponse<TradeOrder> cancel(@PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ApiResponse.ok(service.cancel(id, userId));
    }
}
