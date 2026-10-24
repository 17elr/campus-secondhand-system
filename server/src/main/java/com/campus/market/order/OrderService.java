package com.campus.market.order;

import static com.campus.market.common.Errors.*;

import com.campus.market.auth.*;
import com.campus.market.common.*;
import com.campus.market.goods.*;
import com.campus.market.notice.Outbox;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
  private final OrderMapper orders;
  private final GoodsMapper goods;
  private final UserMapper users;
  private final Outbox outbox;
  private final GoodsCache cache;

  public OrderService(
      OrderMapper orders, GoodsMapper goods, UserMapper users, Outbox outbox, GoodsCache cache) {
    this.orders = orders;
    this.goods = goods;
    this.users = users;
    this.outbox = outbox;
    this.cache = cache;
  }

  @Transactional
  public TradeOrder create(Long gid, Long uid, String key) {
    if (gid == null || gid < 1 || key == null || !key.matches("[A-Za-z0-9_-]{8,80}"))
      throw bad("商品ID或请求标识无效");
    User buyer = users.lock(uid);
    if (buyer == null || buyer.getStatus() != 1) throw forbidden("账号不可用");
    String hash = hash(gid.toString());
    TradeOrder previous = orders.request(uid, key);
    if (previous != null) {
      if (!hash.equals(previous.getRequestHash())) throw conflict("相同请求标识不能用于其他商品");
      return previous;
    }
    Goods g = goods.lock(gid);
    if (g == null) throw missing("商品不存在");
    if (!Boolean.TRUE.equals(g.getOwnershipVerified()) || g.getSellerId() == null)
      throw conflict("历史归属待核对，当前只读");
    if (uid.equals(g.getSellerId())) throw bad("不能购买自己的商品");
    if (!"ON_SALE".equals(g.getStatus())) throw conflict("商品已被预留或不可购买");
    User seller = users.findById(g.getSellerId());
    if (seller == null || seller.getStatus() != 1) throw conflict("卖家账号不可交易");
    TradeOrder o = new TradeOrder();
    o.setGoodsId(gid);
    o.setBuyerId(uid);
    o.setSellerId(g.getSellerId());
    o.setBuyerName(buyer.getNickname());
    o.setSellerName(g.getSellerName());
    o.setTitle(g.getTitle());
    o.setPrice(g.getPrice());
    o.setLocation(g.getLocation());
    o.setRequestKey(key);
    o.setRequestHash(hash);
    orders.insert(o);
    if (goods.reserve(gid, o.getId()) != 1) throw conflict("商品已被预留");
    outbox.add(o.getSellerId(), "ORDER_CREATED", o.getId(), "收到购买意向", o.getTitle());
    cache.evict(gid);
    return orders.find(o.getId());
  }

  public PageResult<TradeOrder> page(PageQuery q, Long uid) {
    return new PageResult<>(
        orders.page(q, uid), orders.pageCount(q, uid), q.getPage(), q.getSize());
  }

  @Transactional
  public TradeOrder transition(Long id, User actor, String action, String reason) {
    // Only immutable association IDs may be read before the fixed buyer -> goods -> order lock
    // sequence.
    TradeOrder link = orders.find(id);
    if (link == null) throw missing("购买意向不存在");
    if (!Boolean.TRUE.equals(link.getOwnershipVerified()) || link.getBuyerId() == null)
      throw conflict("历史订单归属待核对，当前只读");
    users.lock(link.getBuyerId());
    Goods g = goods.lock(link.getGoodsId());
    TradeOrder o = orders.lock(id);
    boolean buyer = actor.getId().equals(o.getBuyerId()),
        seller = actor.getId().equals(o.getSellerId()),
        admin = "ADMIN".equals(actor.getRole());
    String next;
    switch (action) {
      case "confirm":
        if (!seller) throw forbidden("只有卖家可以确认");
        if (!"PENDING".equals(o.getStatus())) throw conflict("仅待确认意向可确认");
        next = "CONFIRMED";
        break;
      case "complete":
        if (!buyer) throw forbidden("只有买家可以确认交接");
        if (!"CONFIRMED".equals(o.getStatus())) throw conflict("请先等待卖家确认");
        next = "COMPLETED";
        break;
      case "cancel":
        if (!buyer && !seller) throw forbidden("只能取消自己的意向");
        if (!"PENDING".equals(o.getStatus())) throw conflict("当前状态不可自行取消");
        next = "CANCELLED";
        reason = buyer ? "买家取消" : "卖家拒绝";
        break;
      case "force-cancel":
        if (!admin) throw forbidden("需要管理员权限");
        if (reason == null || reason.isBlank() || reason.length() > 500)
          throw bad("必须填写1至500字处理原因");
        if (!Set.of("PENDING", "CONFIRMED").contains(o.getStatus())) throw conflict("只能关闭活动意向");
        next = "CANCELLED";
        break;
      default:
        throw bad("无效的意向操作");
    }
    if (g == null
        || !"RESERVED".equals(g.getStatus())
        || !Objects.equals(g.getReservedOrderId(), o.getId())) throw conflict("商品预留归属不一致，请联系管理员");
    if (orders.transition(id, o.getStatus(), next, reason) != 1) throw conflict("意向状态已变化");
    if (!next.equals("CONFIRMED")
        && goods.finish(g.getId(), id, next.equals("COMPLETED") ? "SOLD" : "ON_SALE") != 1)
      throw conflict("商品状态已变化");
    if (action.equals("force-cancel")) outbox.audit(actor.getId(), "FORCE_CANCEL", id, reason);
    Long recipient = seller ? o.getBuyerId() : o.getSellerId();
    outbox.add(recipient, "ORDER_" + next, id, "购买意向状态更新", o.getTitle() + "：" + next);
    if (action.equals("force-cancel"))
      outbox.add(o.getBuyerId(), "ORDER_CANCELLED", id, "意向已由管理员关闭", reason);
    cache.evict(g.getId());
    return orders.find(id);
  }

  private static String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
