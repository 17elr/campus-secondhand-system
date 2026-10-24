package com.campus.market;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.campus.market.auth.*;
import com.campus.market.goods.*;
import com.campus.market.order.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

class TradeConsistencyTest extends MarketTestSupport {
  @Autowired GoodsService service;
  @Autowired GoodsMapper goods;
  @Autowired OrderService orders;
  @Autowired UserMapper users;
  @Autowired JdbcTemplate jdbc;

  Long good() {
    Goods g = new Goods();
    g.setTitle("事务测试" + System.nanoTime());
    g.setPrice(new BigDecimal("12.34"));
    g.setCategory("教材书籍");
    g.setConditionLevel("九成新");
    g.setDescription("测试商品");
    g.setLocation("图书馆");
    return service.create(g, users.findByUsername("zhou").getId()).getId();
  }

  @Test
  void twoBuyersCanCreateOnlyOneActiveOrder() throws Exception {
    Long gid = good();
    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch start = new CountDownLatch(1);
    try {
      List<Future<Boolean>> futures = new ArrayList<>();
      for (String name : List.of("student", "chen"))
        futures.add(
            pool.submit(
                () -> {
                  start.await();
                  try {
                    orders.create(
                        gid, users.findByUsername(name).getId(), UUID.randomUUID().toString());
                    return true;
                  } catch (ResponseStatusException e) {
                    assertEquals(409, e.getStatusCode().value());
                    return false;
                  }
                }));
      start.countDown();
      int success = 0;
      for (Future<Boolean> f : futures) if (f.get(15, TimeUnit.SECONDS)) success++;
      assertEquals(1, success);
      assertEquals(
          1,
          jdbc.queryForObject(
              "select count(*) from trade_order where active_goods_id=?", Integer.class, gid));
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void twentyConcurrentIdenticalRequestsReturnSameOrder() throws Exception {
    Long gid = good(), uid = users.findByUsername("student").getId();
    String key = UUID.randomUUID().toString();
    ExecutorService pool = Executors.newFixedThreadPool(10);
    try {
      List<Callable<Long>> calls = new ArrayList<>();
      for (int i = 0; i < 20; i++) calls.add(() -> orders.create(gid, uid, key).getId());
      Set<Long> ids = new HashSet<>();
      for (Future<Long> f : pool.invokeAll(calls)) ids.add(f.get());
      assertEquals(1, ids.size());
      ResponseStatusException e =
          assertThrows(ResponseStatusException.class, () -> orders.create(good(), uid, key));
      assertEquals(409, e.getStatusCode().value());
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void outboxFailureRollsBackOrderAndReservation() {
    Long gid = good(), uid = users.findByUsername("student").getId();
    assertTrue(jdbc.queryForObject("select database()", String.class).endsWith("_test"));
    Long maxOrder = jdbc.queryForObject("select coalesce(max(id),0) from trade_order", Long.class);
    jdbc.execute(
        "ALTER TABLE outbox_event ADD CONSTRAINT test_outbox_failure CHECK(type<>'ORDER_CREATED' OR"
            + " business_id<="
            + maxOrder
            + ")");
    try {
      assertThrows(
          org.springframework.dao.DataAccessException.class,
          () -> orders.create(gid, uid, UUID.randomUUID().toString()));
    } finally {
      jdbc.execute("ALTER TABLE outbox_event DROP CHECK test_outbox_failure");
    }
    assertEquals("ON_SALE", goods.findById(gid, uid).getStatus());
    assertEquals(
        0,
        jdbc.queryForObject(
            "select count(*) from trade_order where goods_id=?", Integer.class, gid));
  }

  @Test
  void stateMachineOwnershipAndOldCancelCannotReopenSoldGoods() {
    Long gid = good();
    User buyer = users.findByUsername("student"),
        seller = users.findByUsername("zhou"),
        stranger = users.findByUsername("chen"),
        admin = users.findByUsername("admin");
    TradeOrder o = orders.create(gid, buyer.getId(), UUID.randomUUID().toString());
    assertThrows(
        ResponseStatusException.class,
        () -> orders.transition(o.getId(), stranger, "cancel", null));
    assertThrows(
        ResponseStatusException.class, () -> orders.transition(o.getId(), buyer, "complete", null));
    orders.transition(o.getId(), seller, "confirm", null);
    assertThrows(
        ResponseStatusException.class, () -> orders.transition(o.getId(), buyer, "cancel", null));
    assertThrows(
        ResponseStatusException.class,
        () -> orders.transition(o.getId(), admin, "force-cancel", ""));
    orders.transition(o.getId(), buyer, "complete", null);
    assertEquals("SOLD", goods.findById(gid, buyer.getId()).getStatus());
    assertThrows(
        ResponseStatusException.class, () -> orders.transition(o.getId(), buyer, "cancel", null));
    assertEquals("SOLD", goods.findById(gid, buyer.getId()).getStatus());
  }

  @Test
  void mismatchedReservationIsNeverReleased() {
    Long gid = good();
    User buyer = users.findByUsername("student");
    TradeOrder o = orders.create(gid, buyer.getId(), UUID.randomUUID().toString());
    jdbc.update("update goods set reserved_order_id=reserved_order_id+10000 where id=?", gid);
    assertThrows(
        ResponseStatusException.class, () -> orders.transition(o.getId(), buyer, "cancel", null));
    assertEquals("RESERVED", goods.findById(gid, buyer.getId()).getStatus());
  }

  @Test
  void databaseUniqueConstraintReallyRejectsSecondActiveOrder() {
    Long gid = good();
    User buyer = users.findByUsername("student");
    orders.create(gid, buyer.getId(), UUID.randomUUID().toString());
    assertThrows(
        org.springframework.dao.DataIntegrityViolationException.class,
        () ->
            jdbc.update(
                "insert into"
                    + " trade_order(goods_id,title,price,buyer_name,seller_name,status,ownership_verified,created_at)"
                    + " values(?,'duplicate',1,'buyer','seller','PENDING',TRUE,CURRENT_TIMESTAMP)",
                gid));
  }

  @Test
  void repeatedAndConcurrentFavoritesKeepRelationCountCorrect() throws Exception {
    Long gid = good(), uid = users.findByUsername("student").getId();
    ExecutorService pool = Executors.newFixedThreadPool(6);
    try {
      for (boolean desired : List.of(true, false)) {
        List<Callable<Goods>> calls = new ArrayList<>();
        for (int i = 0; i < 20; i++) calls.add(() -> service.favorite(gid, uid, desired));
        for (Future<Goods> f : pool.invokeAll(calls)) f.get();
        assertEquals(desired ? 1 : 0, goods.findById(gid, uid).getFavoriteCount());
        assertEquals(desired ? 1 : 0, goods.favoriteExists(gid, uid));
      }
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void administratorCanCloseConfirmedIntentWithAudit() {
    Long gid = good();
    User buyer = users.findByUsername("student"),
        seller = users.findByUsername("zhou"),
        admin = users.findByUsername("admin");
    TradeOrder o = orders.create(gid, buyer.getId(), UUID.randomUUID().toString());
    orders.transition(o.getId(), seller, "confirm", null);
    orders.transition(o.getId(), admin, "force-cancel", "双方协商取消");
    assertEquals("ON_SALE", goods.findById(gid, buyer.getId()).getStatus());
    assertEquals(
        1,
        jdbc.queryForObject(
            "select count(*) from operation_log where action='FORCE_CANCEL' and target_id=?",
            Integer.class,
            o.getId()));
  }
}
