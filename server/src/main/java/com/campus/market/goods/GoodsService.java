package com.campus.market.goods;

import static com.campus.market.common.Errors.*;

import com.campus.market.auth.*;
import com.campus.market.common.*;
import com.campus.market.order.OrderMapper;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoodsService {
  private final GoodsMapper mapper;
  private final UserMapper users;
  private final GoodsCache cache;
  private final OrderMapper orders;

  public GoodsService(GoodsMapper mapper, UserMapper users, GoodsCache cache, OrderMapper orders) {
    this.mapper = mapper;
    this.users = users;
    this.cache = cache;
    this.orders = orders;
  }

  public Goods get(Long id, Long userId) {
    Goods g = cache.get(id);
    if (g == null) {
      g = mapper.findById(id, 0L);
      if (g == null) throw missing("商品不存在");
      if ("ON_SALE".equals(g.getStatus())) cache.save(g);
    }
    if (!"ON_SALE".equals(g.getStatus())
        && !Objects.equals(userId, g.getSellerId())
        && orders.participates(id, userId) == 0
        && mapper.favoriteExists(id, userId) == 0) {
      User u = users.findById(userId);
      if (u == null || !"ADMIN".equals(u.getRole())) throw missing("商品暂不可见");
    }
    g.setFavorite(mapper.favoriteExists(id, userId) > 0);
    return g;
  }

  public PageResult<Goods> page(PageQuery q, Long userId, String scope) {
    return new PageResult<>(
        mapper.page(q, userId, scope),
        mapper.pageCount(q, userId, scope),
        q.getPage(),
        q.getSize());
  }

  @Transactional
  public Goods create(Goods g, Long userId) {
    g.setId(null);
    g.setSellerId(userId);
    g.setSellerName(users.findById(userId).getNickname());
    image(g);
    mapper.insert(g);
    return mapper.findById(g.getId(), userId);
  }

  @Transactional
  public Goods update(Goods g, Long userId) {
    Goods old = owned(g.getId(), userId);
    editable(old);
    g.setSellerId(userId);
    image(g);
    if (mapper.update(g) != 1) throw conflict("商品状态已变化");
    cache.evict(g.getId());
    return mapper.findById(g.getId(), userId);
  }

  @Transactional
  public Goods updateStatus(Long id, Long userId, String status) {
    Goods g = owned(id, userId);
    change(g, status);
    return mapper.findById(id, userId);
  }

  @Transactional
  public void adminStatus(Long id, String status) {
    Goods g = mapper.lock(id);
    if (g == null) throw missing("商品不存在");
    change(g, status);
  }

  private void change(Goods g, String status) {
    if (!Set.of("ON_SALE", "OFF_SHELF").contains(status)) throw bad("只允许上架或下架");
    editable(g);
    if (mapper.updateStatus(g.getId(), g.getSellerId(), status) != 1) throw conflict("商品状态已变化");
    cache.evict(g.getId());
  }

  private Goods owned(Long id, Long uid) {
    Goods g = mapper.lock(id);
    if (g == null) throw missing("商品不存在");
    if (!Objects.equals(uid, g.getSellerId())) throw forbidden("只能操作自己的商品");
    return g;
  }

  private void editable(Goods g) {
    if (!Boolean.TRUE.equals(g.getOwnershipVerified())) throw conflict("历史归属待核对，当前只读");
    if (!Set.of("ON_SALE", "OFF_SHELF").contains(g.getStatus()) || g.getReservedOrderId() != null)
      throw conflict("预留或已售商品不可编辑或上下架");
  }

  private void image(Goods g) {
    String image = g.getImageUrl();
    if (image != null
        && !image.isBlank()
        && !image.matches("/(images/goods/[0-9]{3}\\.webp|uploads/[a-f0-9-]+\\.(png|jpg))"))
      throw bad("请使用上传生成的图片地址");
  }

  @Transactional
  public Goods favorite(Long id, Long uid, boolean desired) {
    Goods g = mapper.lock(id);
    if (g == null) throw missing("商品不存在");
    int exists = mapper.favoriteExists(id, uid);
    if (desired && exists == 0) {
      if (!"ON_SALE".equals(g.getStatus())) throw missing("商品不可收藏");
      mapper.addFavorite(id, uid);
      mapper.countFavorite(id, 1);
    } else if (!desired && exists > 0) {
      if (mapper.removeFavorite(id, uid) == 1) mapper.countFavorite(id, -1);
    }
    cache.evict(id);
    return mapper.findById(id, uid);
  }
}
