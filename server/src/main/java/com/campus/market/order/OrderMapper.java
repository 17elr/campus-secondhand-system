package com.campus.market.order;

import org.apache.ibatis.annotations.*;
import java.util.List;
import com.campus.market.common.PageQuery;

@Mapper
public interface OrderMapper {
    String PAGE_WHERE = " from trade_order where buyer_name=#{buyerName} <if test=\"q.status != ''\">and status=#{q.status}</if> <if test=\"q.keyword != ''\">and (title like concat('%',#{q.keyword},'%') or seller_name like concat('%',#{q.keyword},'%'))</if>";

    @Select("<script>select *" + PAGE_WHERE + " order by id desc limit #{q.size} offset #{q.offset}</script>")
    List<TradeOrder> page(@Param("q") PageQuery query, @Param("buyerName") String buyerName);

    @Select("<script>select count(*)" + PAGE_WHERE + "</script>")
    long pageCount(@Param("q") PageQuery query, @Param("buyerName") String buyerName);

    @Insert("insert into trade_order(goods_id,title,price,buyer_name,seller_name,location,status,created_at) values(#{goodsId},#{title},#{price},#{buyerName},#{sellerName},#{location},'PENDING',CURRENT_TIMESTAMP)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TradeOrder order);

    @Select("select id,goods_id goodsId,title,price,buyer_name buyerName,seller_name sellerName,location,status,created_at createdAt from trade_order where buyer_name=#{buyerName} order by id desc")
    List<TradeOrder> mine(String buyerName);

    @Update("update trade_order set status=#{status} where id=#{id} and buyer_name=#{buyerName}")
    int updateStatus(@Param("id") Long id, @Param("buyerName") String buyerName,
            @Param("status") String status);
}
