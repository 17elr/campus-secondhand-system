package com.campus.market.goods;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.*;

@Component
public class GoodsCache {
  private final StringRedisTemplate redis;
  private final ObjectMapper json;

  public GoodsCache(StringRedisTemplate redis, ObjectMapper json) {
    this.redis = redis;
    this.json = json;
  }

  private String key(Long id) {
    return "market:goods:v1:" + id;
  }

  public Goods get(Long id) {
    try {
      String value = redis.opsForValue().get(key(id));
      return value == null ? null : json.readValue(value, Goods.class);
    } catch (Exception e) {
      return null;
    }
  }

  public void save(Goods goods) {
    try {
      com.fasterxml.jackson.databind.node.ObjectNode view = json.valueToTree(goods);
      view.remove("favorite");
      redis
          .opsForValue()
          .set(
              key(goods.getId()),
              json.writeValueAsString(view),
              Duration.ofSeconds(30 + ThreadLocalRandom.current().nextInt(11)));
    } catch (Exception ignored) {
    }
  }

  public void evict(Long id) {
    Runnable action =
        () -> {
          try {
            redis.delete(key(id));
          } catch (Exception ignored) {
          }
        };
    if (TransactionSynchronizationManager.isSynchronizationActive())
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              action.run();
            }
          });
    else action.run();
  }
}
