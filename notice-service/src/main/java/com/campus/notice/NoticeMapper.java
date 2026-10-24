package com.campus.notice;

import java.util.*;
import org.apache.ibatis.annotations.*;

@Mapper
public interface NoticeMapper {
  @Insert(
      "insert into"
          + " notification(event_id,recipient_id,type,business_id,title,body,occurred_at,payload_hash)"
          + " values(#{event},#{n.recipientId},#{n.type},#{n.businessId},#{n.title},#{n.body},#{n.occurredAt},#{hash})"
          + " on duplicate key update id=LAST_INSERT_ID(id)")
  int insert(@Param("event") String event, @Param("n") Notification n, @Param("hash") String hash);

  @Select("select * from notification where event_id=#{event} and recipient_id=#{recipient}")
  Map<String, Object> event(@Param("event") String event, @Param("recipient") Long recipient);

  @Select(
      "select id,event_id eventId,type,business_id businessId,title,body,is_read isRead,occurred_at"
          + " occurredAt from notification where recipient_id=#{user} order by id desc limit"
          + " #{size} offset #{offset}")
  List<Map<String, Object>> page(
      @Param("user") Long user, @Param("size") int size, @Param("offset") long offset);

  @Select("select count(*) from notification where recipient_id=#{user}")
  long count(Long user);

  @Select("select count(*) from notification where recipient_id=#{user} and is_read=FALSE")
  long unread(Long user);

  @Update("update notification set is_read=TRUE where id=#{id} and recipient_id=#{user}")
  int read(@Param("user") Long user, @Param("id") Long id);

  @Select("select count(*) from notification where id=#{id} and recipient_id=#{user}")
  int owns(@Param("user") Long user, @Param("id") Long id);
}
