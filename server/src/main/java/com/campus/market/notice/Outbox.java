package com.campus.market.notice;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class Outbox {
  private final JdbcTemplate jdbc;
  private final ObjectMapper json;
  private final NoticeClient client;
  private final TransactionTemplate tx;
  private final String token;

  public Outbox(
      JdbcTemplate jdbc,
      ObjectMapper json,
      NoticeClient client,
      PlatformTransactionManager manager,
      @Value("${notice.internal-token}") String token) {
    this.jdbc = jdbc;
    this.json = json;
    this.client = client;
    this.tx = new TransactionTemplate(manager);
    this.token = token;
    if (token.length() < 32)
      throw new IllegalArgumentException(
          "NOTICE_INTERNAL_TOKEN must contain at least 32 characters");
  }

  public void add(Long recipient, String type, Long businessId, String title, String body) {
    if (recipient == null) return;
    try {
      Map<String, Object> payload =
          Map.of(
              "recipientId",
              recipient,
              "type",
              type,
              "businessId",
              businessId,
              "title",
              title,
              "body",
              body,
              "occurredAt",
              Instant.now().toString());
      jdbc.update(
          "insert into outbox_event(event_id,recipient_id,type,business_id,payload)"
              + " values(?,?,?,?,?)",
          UUID.randomUUID().toString(),
          recipient,
          type,
          businessId,
          json.writeValueAsString(payload));
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  public void audit(Long actor, String action, Long target, String reason) {
    if (reason == null || reason.isBlank() || reason.length() > 500)
      throw com.campus.market.common.Errors.bad("请填写1至500字处理原因");
    jdbc.update(
        "insert into operation_log(actor_id,action,target_id,reason) values(?,?,?,?)",
        actor,
        action,
        target,
        reason);
  }

  @Scheduled(fixedDelayString = "${market.outbox.delay:5000}")
  public void scheduled() {
    if (enabled) dispatch();
  }

  @Value("${market.outbox.enabled:true}")
  private boolean enabled;

  public void dispatch() {
    jdbc.update(
        "update outbox_event set status='FAILED',last_error='Delivery lease expired after final"
            + " attempt' where status='PROCESSING' and lease_until<CURRENT_TIMESTAMP and"
            + " attempt>=3");
    for (int i = 0; i < 20; i++) {
      String owner = UUID.randomUUID().toString();
      Map<String, Object> event =
          tx.execute(
              s -> {
                List<Map<String, Object>> rows =
                    jdbc.queryForList(
                        "select event_id,payload,attempt from outbox_event where attempt<3 and"
                            + " ((status='PENDING' and next_run_at<=CURRENT_TIMESTAMP) or"
                            + " (status='PROCESSING' and lease_until<CURRENT_TIMESTAMP)) order by"
                            + " next_run_at,event_id limit 1 for update skip locked");
                if (rows.isEmpty()) return null;
                Map<String, Object> row = rows.get(0);
                jdbc.update(
                    "update outbox_event set"
                        + " status='PROCESSING',attempt=attempt+1,lease_owner=?,lease_until=DATE_ADD(CURRENT_TIMESTAMP,INTERVAL"
                        + " 15 SECOND) where event_id=?",
                    owner,
                    row.get("event_id"));
                return row;
              });
      if (event == null) return;
      String id = event.get("event_id").toString();
      int attempt = ((Number) event.get("attempt")).intValue() + 1;
      try {
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = json.readValue(event.get("payload").toString(), Map.class);
        client.deliver(token, id, payload);
        jdbc.update(
            "update outbox_event set"
                + " status='DELIVERED',lease_until=null,lease_owner=null,last_error=null where"
                + " event_id=? and lease_owner=?",
            id,
            owner);
      } catch (Exception e) {
        int delay = new int[] {5, 30, 120}[attempt - 1];
        jdbc.update(
            "update outbox_event set status=?,next_run_at=DATE_ADD(CURRENT_TIMESTAMP,INTERVAL ?"
                + " SECOND),lease_until=null,lease_owner=null,last_error=? where event_id=? and"
                + " lease_owner=?",
            attempt >= 3 ? "FAILED" : "PENDING",
            delay,
            e instanceof feign.FeignException f
                ? "HTTP " + f.status()
                : e.getClass().getSimpleName(),
            id,
            owner);
      }
    }
  }
}
