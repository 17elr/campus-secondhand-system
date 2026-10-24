package com.campus.market.message;

import com.campus.market.auth.User;
import com.campus.market.auth.UserMapper;
import com.campus.market.common.ApiResponse;
import com.campus.market.common.PageQuery;
import com.campus.market.common.PageResult;
import com.campus.market.goods.Goods;
import com.campus.market.goods.GoodsMapper;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/messages")
public class GoodsMessageController {
  private final GoodsMessageMapper mapper;
  private final UserMapper userMapper;
  private final GoodsMapper goodsMapper;
  private final com.campus.market.notice.Outbox outbox;
  private final com.campus.market.goods.GoodsService goodsService;

  public GoodsMessageController(
      GoodsMessageMapper mapper,
      UserMapper userMapper,
      GoodsMapper goodsMapper,
      com.campus.market.notice.Outbox outbox,
      com.campus.market.goods.GoodsService goodsService) {
    this.outbox = outbox;
    this.goodsService = goodsService;
    this.mapper = mapper;
    this.userMapper = userMapper;
    this.goodsMapper = goodsMapper;
  }

  @GetMapping("/goods/{goodsId}")
  public ApiResponse<PageResult<GoodsMessage>> list(
      @PathVariable Long goodsId,
      @Valid PageQuery query,
      @RequestAttribute(value = "currentUser", required = false) User viewer) {
    goodsService.get(goodsId, viewer == null ? 0L : viewer.getId());
    return ApiResponse.ok(
        new PageResult<>(
            mapper.page(query, null, goodsId),
            mapper.pageCount(query, null, goodsId),
            query.getPage(),
            query.getSize()));
  }

  @GetMapping("/mine")
  public ApiResponse<PageResult<GoodsMessage>> mine(
      @Valid PageQuery query,
      @RequestAttribute(value = "currentUser", required = false) User currentUser) {
    Long userId = currentUser == null ? 0L : currentUser.getId();
    activeUser(userId);
    return ApiResponse.ok(
        new PageResult<>(
            mapper.page(query, userId, null),
            mapper.pageCount(query, userId, null),
            query.getPage(),
            query.getSize()));
  }

  @org.springframework.transaction.annotation.Transactional
  @PostMapping
  public ApiResponse<GoodsMessage> create(
      @RequestBody GoodsMessage message,
      @RequestAttribute(value = "currentUser", required = false) User currentUser) {
    Long userId = currentUser == null ? 0L : currentUser.getId();
    User user = activeUser(userId);
    String content = content(message.getContent());
    if (message.getGoodsId() == null || goodsMapper.findById(message.getGoodsId(), userId) == null)
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "商品不存在");
    message.setUserId(userId);
    message.setUserName(user.getNickname());
    message.setContent(content);
    message.setId(null);
    Goods target = goodsService.get(message.getGoodsId(), userId);
    if (!Boolean.TRUE.equals(target.getOwnershipVerified()))
      throw com.campus.market.common.Errors.conflict("历史商品归属待核对，当前只读");
    mapper.insert(message);
    outbox.add(target.getSellerId(), "MESSAGE_CREATED", message.getId(), "收到商品咨询", content);
    return ApiResponse.ok(mapper.findById(message.getId()));
  }

  @org.springframework.transaction.annotation.Transactional
  @PatchMapping("/{id}/reply")
  public ApiResponse<Boolean> reply(
      @PathVariable Long id,
      @RequestBody Map<String, String> body,
      @RequestAttribute(value = "currentUser", required = false) User currentUser) {
    Long userId = currentUser == null ? 0L : currentUser.getId();
    User user = activeUser(userId);
    GoodsMessage message = mapper.findById(id);
    if (message == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "留言不存在");
    Goods goods = goodsMapper.findById(message.getGoodsId(), userId);
    if (goods == null || !userId.equals(goods.getSellerId()))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有商品卖家可以回复");
    if (!Boolean.TRUE.equals(goods.getOwnershipVerified()))
      throw com.campus.market.common.Errors.conflict("历史归属待核对，当前只读");
    String reply = content(body.get("content"));
    boolean updated = mapper.reply(id, reply, user.getNickname()) > 0;
    if (updated) outbox.add(message.getUserId(), "MESSAGE_REPLIED", id, "卖家回复了咨询", reply);
    return ApiResponse.ok(updated);
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
