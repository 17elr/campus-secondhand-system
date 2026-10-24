package com.campus.market;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.campus.market.auth.*;
import com.campus.market.goods.*;
import com.campus.market.order.*;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

class TransactionFailureTest extends MarketTestSupport {
  @Autowired GoodsService service;
  @Autowired OrderService orders;
  @Autowired UserMapper users;
  @Autowired JdbcTemplate jdbc;
  @MockitoSpyBean GoodsMapper goods;
  @MockitoSpyBean OrderMapper orderMapper;

  Long good() {
    Goods g = new Goods();
    g.setTitle("回滚验证" + System.nanoTime());
    g.setPrice(new BigDecimal("12.34"));
    g.setCategory("教材书籍");
    g.setConditionLevel("九成新");
    g.setDescription("回滚验证");
    g.setLocation("图书馆");
    return service.create(g, 3L).getId();
  }

  void unchanged(Long gid) {
    assertEquals("ON_SALE", goods.findById(gid, 1L).getStatus());
    assertEquals(
        0,
        jdbc.queryForObject(
            "select count(*) from trade_order where goods_id=?", Integer.class, gid));
  }

  @Test
  void orderInsertFailureLeavesNoReservation() {
    Long gid = good();
    doThrow(new IllegalStateException("insert failed")).when(orderMapper).insert(any());
    assertThrows(
        IllegalStateException.class, () -> orders.create(gid, 1L, UUID.randomUUID().toString()));
    unchanged(gid);
  }

  @Test
  void failedConditionalReservationRollsBackInsertedOrder() {
    Long gid = good();
    doReturn(0).when(goods).reserve(eq(gid), any());
    assertThrows(
        org.springframework.web.server.ResponseStatusException.class,
        () -> orders.create(gid, 1L, UUID.randomUUID().toString()));
    unchanged(gid);
  }

  @Test
  void favoriteCountFailureRollsBackRelation() {
    Long gid = good();
    doThrow(new IllegalStateException("counter failed")).when(goods).countFavorite(gid, 1);
    assertThrows(IllegalStateException.class, () -> service.favorite(gid, 1L, true));
    assertEquals(0, goods.favoriteExists(gid, 1L));
    assertEquals(0, goods.findById(gid, 1L).getFavoriteCount());
  }

  @Test
  void cacheDoesNotLeakPersonalFavoriteFlags() {
    Long gid = good();
    service.favorite(gid, 1L, true);
    assertTrue(service.get(gid, 1L).getFavorite());
    assertFalse(service.get(gid, 4L).getFavorite());
    assertFalse(service.get(gid, 0L).getFavorite());
  }
}
