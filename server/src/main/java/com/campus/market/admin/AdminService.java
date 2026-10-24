package com.campus.market.admin;

import static com.campus.market.common.Errors.*;

import com.campus.market.auth.*;
import com.campus.market.goods.GoodsService;
import com.campus.market.message.*;
import com.campus.market.notice.Outbox;
import com.campus.market.order.OrderService;
import com.campus.market.want.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
  private final JdbcTemplate jdbc;
  private final GoodsService goods;
  private final OrderService orders;
  private final Outbox outbox;
  private final GoodsMessageMapper messages;
  private final WantMapper wants;

  public AdminService(
      JdbcTemplate jdbc,
      GoodsService goods,
      OrderService orders,
      Outbox outbox,
      GoodsMessageMapper messages,
      WantMapper wants) {
    this.jdbc = jdbc;
    this.goods = goods;
    this.orders = orders;
    this.outbox = outbox;
    this.messages = messages;
    this.wants = wants;
  }

  @Transactional
  public void goods(Long id, String value, User actor, String reason) {
    outbox.audit(actor.getId(), "GOODS_" + value, id, reason);
    goods.adminStatus(id, value);
  }

  @Transactional
  public void user(Long id, Integer value, User actor, String reason) {
    if (value == null || value < 0 || value > 1) throw bad("无效的账号状态");
    if (id.equals(actor.getId())) throw bad("不能停用当前管理员");
    if (jdbc.update(
            "update sys_user set status=?,auth_version=auth_version+1 where id=?", value, id)
        != 1) throw missing("用户不存在");
    outbox.audit(actor.getId(), "USER_STATUS", id, reason);
  }

  @Transactional
  public void role(Long id, String value, User actor, String reason) {
    if (!Set.of("USER", "ADMIN").contains(value)) throw bad("无效的角色");
    if (id.equals(actor.getId())) throw bad("不能修改当前管理员角色");
    if (jdbc.update("update sys_user set role=?,auth_version=auth_version+1 where id=?", value, id)
        != 1) throw missing("用户不存在");
    outbox.audit(actor.getId(), "USER_ROLE", id, reason);
  }

  public void order(Long id, String value, User actor, String reason) {
    if (!"CANCELLED".equals(value)) throw bad("管理员仅可说明原因后关闭活动意向，完成须由买家确认");
    orders.transition(id, actor, "force-cancel", reason);
  }

  @Transactional
  public void report(Long id, String value, User actor, String reason) {
    if (!Set.of("RESOLVED", "IGNORED").contains(value)) throw bad("无效的举报状态");
    if (jdbc.update("update report set status=? where id=? and status='PENDING'", value, id) != 1)
      throw conflict("举报不存在或已处理");
    outbox.audit(actor.getId(), "REPORT_" + value, id, reason);
  }

  @Transactional
  public void want(Long id, String value, User actor, String reason) {
    if (!Set.of("PUBLISHED", "HIDDEN").contains(value)) throw bad("无效的审核状态");
    Want w = wants.findById(id);
    if (w == null) throw missing("求购不存在");
    if (jdbc.update(
            "update want_post set status=? where id=? and status in"
                + " ('PENDING','PUBLISHED','HIDDEN')",
            value,
            id)
        != 1) throw conflict("求购已撤回或结束");
    outbox.audit(actor.getId(), "WANT_" + value, id, reason);
    outbox.add(w.getUserId(), "WANT_REVIEWED", id, "求购审核结果", w.getTitle() + "：" + value);
  }

  @Transactional
  public void reply(Long id, String content, User actor) {
    if (content == null || content.isBlank() || content.length() > 500) throw bad("回复应为1至500字");
    GoodsMessage m = messages.findById(id);
    if (m == null) throw missing("咨询不存在");
    messages.reply(id, content.trim(), actor.getNickname());
    outbox.audit(actor.getId(), "MESSAGE_REPLY", id, "管理员回复咨询");
    outbox.add(m.getUserId(), "MESSAGE_REPLIED", id, "管理员回复了咨询", content.trim());
  }

  @Transactional
  public void retry(String id, User actor, String reason) {
    if (jdbc.update(
            "update outbox_event set"
                + " status='PENDING',attempt=0,next_run_at=CURRENT_TIMESTAMP,lease_owner=null,lease_until=null"
                + " where event_id=? and status='FAILED'",
            id)
        != 1) throw conflict("仅失败事件可补投");
    outbox.audit(actor.getId(), "OUTBOX_RETRY", 0L, reason + " [" + id + "]");
  }
}
