package com.campus.market.goods;

import com.campus.market.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.campus.market.common.PageQuery;
import com.campus.market.common.PageResult;

@RestController
@RequestMapping("/api/goods")
public class GoodsController {
    private final GoodsService service;

    public GoodsController(GoodsService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResult<Goods>> list(@Valid PageQuery query,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ApiResponse.ok(service.page(query, userId, "market"));
    }

    @GetMapping("/mine")
    public ApiResponse<PageResult<Goods>> mine(@Valid PageQuery query,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ApiResponse.ok(service.page(query, userId, "mine"));
    }

    @GetMapping("/favorites")
    public ApiResponse<PageResult<Goods>> favorites(@Valid PageQuery query,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ApiResponse.ok(service.page(query, userId, "favorites"));
    }

    @GetMapping("/{id}")
    public ApiResponse<Goods> get(@PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ApiResponse.ok(service.get(id, userId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Goods> create(@Valid @RequestBody Goods goods,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ApiResponse.ok(service.create(goods, userId));
    }

    @PutMapping("/{id}")
    public ApiResponse<Goods> update(@PathVariable Long id, @Valid @RequestBody Goods goods,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        goods.setId(id);
        return ApiResponse.ok(service.update(goods, userId));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<Goods> status(@PathVariable Long id, @RequestParam String value,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ApiResponse.ok(service.updateStatus(id, userId, value));
    }

    @PostMapping("/{id}/favorite")
    public ApiResponse<Goods> favorite(@PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ApiResponse.ok(service.toggleFavorite(id, userId));
    }
}
