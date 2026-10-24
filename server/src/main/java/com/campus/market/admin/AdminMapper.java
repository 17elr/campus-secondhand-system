package com.campus.market.admin;

import com.campus.market.common.PageQuery;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AdminMapper {
  @Select(
      "<script>select id,title,price,category,condition_level"
          + " conditionLevel,location,description,image_url imageUrl,seller_name"
          + " sellerName,status,favorite_count favoriteCount,created_at createdAt from goods where"
          + " 1=1 <if test='q.keyword != null and q.keyword != &quot;&quot;'>and (title like"
          + " concat('%',#{q.keyword},'%') or seller_name like concat('%',#{q.keyword},'%'))</if>"
          + " <if test='q.status != null and q.status != &quot;&quot;'>and status=#{q.status}</if>"
          + " order by id desc limit #{q.size} offset #{q.offset}</script>")
  List<Map<String, Object>> goodsPage(@Param("q") PageQuery query);

  @Select(
      "<script>select count(*) from goods where 1=1 <if test='q.keyword != null and q.keyword !="
          + " &quot;&quot;'>and (title like concat('%',#{q.keyword},'%') or seller_name like"
          + " concat('%',#{q.keyword},'%'))</if> <if test='q.status != null and q.status !="
          + " &quot;&quot;'>and status=#{q.status}</if></script>")
  long goodsPageCount(@Param("q") PageQuery query);

  @Select(
      "<script>select id,username,nickname,role,status,created_at createdAt from sys_user where 1=1"
          + " <if test='q.keyword != null and q.keyword != &quot;&quot;'>and (username like"
          + " concat('%',#{q.keyword},'%') or nickname like concat('%',#{q.keyword},'%'))</if>"
          + " order by id desc limit #{q.size} offset #{q.offset}</script>")
  List<Map<String, Object>> usersPage(@Param("q") PageQuery query);

  @Select(
      "<script>select count(*) from sys_user where 1=1 <if test='q.keyword != null and q.keyword !="
          + " &quot;&quot;'>and (username like concat('%',#{q.keyword},'%') or nickname like"
          + " concat('%',#{q.keyword},'%'))</if></script>")
  long usersPageCount(@Param("q") PageQuery query);

  @Select(
      "<script>select id,goods_id goodsId,title,price,buyer_name buyerName,seller_name"
          + " sellerName,location,status,created_at createdAt from trade_order where 1=1 <if"
          + " test='q.status != null and q.status != &quot;&quot;'>and status=#{q.status}</if> <if"
          + " test='q.keyword != null and q.keyword != &quot;&quot;'>and (title like"
          + " concat('%',#{q.keyword},'%') or buyer_name like concat('%',#{q.keyword},'%') or"
          + " seller_name like concat('%',#{q.keyword},'%'))</if> order by id desc limit #{q.size}"
          + " offset #{q.offset}</script>")
  List<Map<String, Object>> ordersPage(@Param("q") PageQuery query);

  @Select(
      "<script>select count(*) from trade_order where 1=1 <if test='q.status != null and q.status"
          + " != &quot;&quot;'>and status=#{q.status}</if> <if test='q.keyword != null and"
          + " q.keyword != &quot;&quot;'>and (title like concat('%',#{q.keyword},'%') or buyer_name"
          + " like concat('%',#{q.keyword},'%') or seller_name like"
          + " concat('%',#{q.keyword},'%'))</if></script>")
  long ordersPageCount(@Param("q") PageQuery query);

  @Select(
      "<script>select id,reporter_name reporterName,target_type targetType,target_id"
          + " targetId,reason,status,created_at createdAt from report where 1=1 <if test='q.status"
          + " != null and q.status != &quot;&quot;'>and status=#{q.status}</if> order by id desc"
          + " limit #{q.size} offset #{q.offset}</script>")
  List<Map<String, Object>> reportsPage(@Param("q") PageQuery query);

  @Select(
      "<script>select count(*) from report where 1=1 <if test='q.status != null and q.status !="
          + " &quot;&quot;'>and status=#{q.status}</if></script>")
  long reportsPageCount(@Param("q") PageQuery query);

  @Select(
      "<script>select id,user_id userId,user_name messageUserName,goods_id"
          + " goodsId,content,reply_content replyContent,reply_name replyName,status,created_at"
          + " createdAt,replied_at repliedAt from goods_message where 1=1 <if test='q.status !="
          + " null and q.status != &quot;&quot;'>and status=#{q.status}</if> order by id desc limit"
          + " #{q.size} offset #{q.offset}</script>")
  List<Map<String, Object>> messagesPage(@Param("q") PageQuery query);

  @Select(
      "<script>select count(*) from goods_message where 1=1 <if test='q.status != null and q.status"
          + " != &quot;&quot;'>and status=#{q.status}</if></script>")
  long messagesPageCount(@Param("q") PageQuery query);

  @Select(
      "<script>select id,user_id userId,requester_name"
          + " requesterName,title,category,budget,description,status,created_at createdAt from"
          + " want_post where 1=1 <if test='q.status != null and q.status != &quot;&quot;'>and"
          + " status=#{q.status}</if> <if test='q.keyword != null and q.keyword !="
          + " &quot;&quot;'>and (title like concat('%',#{q.keyword},'%') or requester_name like"
          + " concat('%',#{q.keyword},'%'))</if> order by id desc limit #{q.size} offset"
          + " #{q.offset}</script>")
  List<Map<String, Object>> wantsPage(@Param("q") PageQuery query);

  @Select(
      "<script>select count(*) from want_post where 1=1 <if test='q.status != null and q.status !="
          + " &quot;&quot;'>and status=#{q.status}</if> <if test='q.keyword != null and q.keyword"
          + " != &quot;&quot;'>and (title like concat('%',#{q.keyword},'%') or requester_name like"
          + " concat('%',#{q.keyword},'%'))</if></script>")
  long wantsPageCount(@Param("q") PageQuery query);

  @Select("select count(*) from goods")
  int goodsCount();

  @Select("select count(*) from goods where status='ON_SALE'")
  int onSaleCount();

  @Select("select count(*) from trade_order")
  int orderCount();

  @Select("select count(*) from trade_order where status='PENDING'")
  int pendingOrderCount();

  @Select("select count(*) from goods where status='OFF_SHELF'")
  int offShelfCount();

  @Select("select count(*) from sys_user where status=1")
  int activeUserCount();

  @Select("select count(*) from report where status='PENDING'")
  int pendingReportCount();

  @Select("select count(*) from want_post where status='PENDING'")
  int pendingWantCount();
}
