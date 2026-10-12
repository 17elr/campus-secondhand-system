package com.campus.market.message;

import org.apache.ibatis.annotations.*;
import java.util.List;
import com.campus.market.common.PageQuery;

@Mapper
public interface GoodsMessageMapper {
    String PAGE_WHERE = " from goods_message m join goods g on g.id=m.goods_id where m.status&lt;&gt;'HIDDEN' <choose><when test=\"goodsId != null\">and m.goods_id=#{goodsId}</when><otherwise>and (m.user_id=#{userId} or g.seller_name=(select nickname from sys_user where id=#{userId}))</otherwise></choose> <if test=\"q.status != ''\">and m.status=#{q.status}</if> <if test=\"q.onlyMine\">and g.seller_name=(select nickname from sys_user where id=#{userId}) and m.user_id&lt;&gt;#{userId}</if>";

    @Select("<script>select m.*,g.title goodsTitle" + PAGE_WHERE
            + " order by m.id desc limit #{q.size} offset #{q.offset}</script>")
    List<GoodsMessage> page(@Param("q") PageQuery query, @Param("userId") Long userId,
            @Param("goodsId") Long goodsId);

    @Select("<script>select count(*)" + PAGE_WHERE + "</script>")
    long pageCount(@Param("q") PageQuery query, @Param("userId") Long userId, @Param("goodsId") Long goodsId);

    @Select("select id,goods_id goodsId,user_id userId,user_name userName,content,reply_content replyContent,reply_name replyName,status,created_at createdAt,replied_at repliedAt from goods_message where goods_id=#{goodsId} and status<>'HIDDEN' order by id desc")
    List<GoodsMessage> findByGoodsId(Long goodsId);

    @Select("select id,goods_id goodsId,user_id userId,user_name userName,content,reply_content replyContent,reply_name replyName,status,created_at createdAt,replied_at repliedAt from goods_message order by id desc")
    List<GoodsMessage> findAll();

    @Insert("insert into goods_message(goods_id,user_id,user_name,content,status,created_at) values(#{goodsId},#{userId},#{userName},#{content},'PENDING',CURRENT_TIMESTAMP)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(GoodsMessage message);

    @Update("update goods_message set reply_content=#{replyContent},reply_name=#{replyName},status='REPLIED',replied_at=CURRENT_TIMESTAMP where id=#{id}")
    int reply(@Param("id") Long id, @Param("replyContent") String replyContent,
            @Param("replyName") String replyName);

    @Select("select m.*,g.title goodsTitle from goods_message m join goods g on g.id=m.goods_id where m.status<>'HIDDEN' and (m.user_id=#{userId} or g.seller_name=(select nickname from sys_user where id=#{userId})) order by m.id desc")
    List<GoodsMessage> findMine(Long userId);

    @Select("select * from goods_message where id=#{id} and status<>'HIDDEN'")
    GoodsMessage findById(Long id);
}
