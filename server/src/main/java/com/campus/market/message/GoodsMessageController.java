package com.campus.market.message;

import com.campus.market.auth.User;
import com.campus.market.auth.UserMapper;
import com.campus.market.common.ApiResponse;
import com.campus.market.common.PageQuery;
import com.campus.market.common.PageResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import com.campus.market.goods.Goods;
import com.campus.market.goods.GoodsMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/messages")
public class GoodsMessageController {
    private final GoodsMessageMapper mapper;
    private final UserMapper userMapper;
    private final GoodsMapper goodsMapper;

    public GoodsMessageController(GoodsMessageMapper mapper, UserMapper userMapper, GoodsMapper goodsMapper) {
        this.mapper = mapper;
        this.userMapper = userMapper;
        this.goodsMapper = goodsMapper;
    }

    @GetMapping("/goods/{goodsId}")
    public ApiResponse<PageResult<GoodsMessage>> list(@PathVariable Long goodsId, @Valid PageQuery query) {
        return ApiResponse.ok(new PageResult<>(mapper.page(query, null, goodsId),
                mapper.pageCount(query, null, goodsId), query.getPage(), query.getSize()));
    }

    @GetMapping("/mine")
    public ApiResponse<PageResult<GoodsMessage>> mine(@Valid PageQuery query,
            @RequestHeader("X-User-Id") Long userId) {
        activeUser(userId);
        return ApiResponse.ok(new PageResult<>(mapper.page(query, userId, null),
                mapper.pageCount(query, userId, null), query.getPage(), query.getSize()));
    }

    @PostMapping
    public ApiResponse<GoodsMessage> create(@RequestBody GoodsMessage message,
            @RequestHeader("X-User-Id") Long userId) {
        User user = activeUser(userId);
        String content = content(message.getContent());
        if (message.getGoodsId() == null || goodsMapper.findById(message.getGoodsId(), userId) == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "商品不存在");
        message.setUserId(userId);
        message.setUserName(user.getNickname());
        message.setContent(content);
        mapper.insert(message);
        return ApiResponse.ok(mapper.findById(message.getId()));
    }

    @PatchMapping("/{id}/reply")
    public ApiResponse<Boolean> reply(@PathVariable Long id, @RequestBody Map<String, String> body,
            @RequestHeader("X-User-Id") Long userId) {
        User user = activeUser(userId);
        GoodsMessage message = mapper.findById(id);
        if (message == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "留言不存在");
        Goods goods = goodsMapper.findById(message.getGoodsId(), userId);
        if (goods == null || !user.getNickname().equals(goods.getSellerName()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有商品卖家可以回复");
        return ApiResponse.ok(mapper.reply(id, content(body.get("content")), user.getNickname()) > 0);
    }

    private User activeUser(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null || user.getStatus() != 1)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "账号不可用");
        return user;
    }

    private String content(String value) {
        if (value == null || value.isBlank() || value.trim().length() > 500)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "留言或回复应为 1 至 500 字");
        return value.trim();
    }
}
