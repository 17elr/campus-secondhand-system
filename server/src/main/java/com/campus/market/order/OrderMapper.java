package com.campus.market.order;

import com.campus.market.common.PageQuery;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface OrderMapper {
  String WHERE =
      " from trade_order where (buyer_id=#{userId} or seller_id=#{userId}) <if test=\"q.status !="
          + " ''\">and status=#{q.status}</if> <if test=\"q.keyword != ''\">and (title like"
          + " concat('%',#{q.keyword},'%') or seller_name like concat('%',#{q.keyword},'%') or"
          + " buyer_name like concat('%',#{q.keyword},'%'))</if>";

  @Select(
      "<script>select *" + WHERE + " order by id desc limit #{q.size} offset #{q.offset}</script>")
  List<TradeOrder> page(@Param("q") PageQuery q, @Param("userId") Long userId);

  @Select("<script>select count(*)" + WHERE + "</script>")
  long pageCount(@Param("q") PageQuery q, @Param("userId") Long userId);

  @Select("select * from trade_order where id=#{id}")
  TradeOrder find(Long id);

  @Select("select * from trade_order where id=#{id} for update")
  TradeOrder lock(Long id);

  @Select("select * from trade_order where buyer_id=#{buyerId} and request_key=#{key}")
  TradeOrder request(@Param("buyerId") Long buyerId, @Param("key") String key);

  @Select(
      "select count(*) from trade_order where goods_id=#{goodsId} and (buyer_id=#{userId} or"
          + " seller_id=#{userId})")
  int participates(@Param("goodsId") Long goodsId, @Param("userId") Long userId);

  @Insert(
      "insert into"
          + " trade_order(goods_id,title,price,buyer_id,seller_id,buyer_name,seller_name,location,status,request_key,request_hash,ownership_verified,created_at)"
          + " values(#{goodsId},#{title},#{price},#{buyerId},#{sellerId},#{buyerName},#{sellerName},#{location},'PENDING',#{requestKey},#{requestHash},TRUE,CURRENT_TIMESTAMP)")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insert(TradeOrder o);

  @Update(
      "update trade_order set status=#{status},confirmed_at=case when #{status}='CONFIRMED' then"
          + " CURRENT_TIMESTAMP else confirmed_at end,completed_at=case when #{status}='COMPLETED'"
          + " then CURRENT_TIMESTAMP else completed_at end,cancelled_at=case when"
          + " #{status}='CANCELLED' then CURRENT_TIMESTAMP else cancelled_at"
          + " end,cancel_reason=#{reason} where id=#{id} and status=#{old}")
  int transition(
      @Param("id") Long id,
      @Param("old") String old,
      @Param("status") String status,
      @Param("reason") String reason);
}
