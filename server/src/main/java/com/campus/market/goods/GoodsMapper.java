package com.campus.market.goods;

import com.campus.market.common.PageQuery;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface GoodsMapper {
  String PAGE_WHERE =
      " from goods g where 1=1 <choose><when test=\"scope == 'mine'\">and"
          + " g.seller_id=#{userId}</when><when test=\"scope == 'favorites'\">and exists(select 1"
          + " from favorite where goods_id=g.id and user_id=#{userId})</when><otherwise>and"
          + " g.status='ON_SALE'</otherwise></choose> <if test=\"q.keyword != ''\">and (g.title"
          + " like concat('%',#{q.keyword},'%') or g.description like concat('%',#{q.keyword},'%')"
          + " or g.seller_name like concat('%',#{q.keyword},'%') or g.location like"
          + " concat('%',#{q.keyword},'%'))</if> <if test=\"q.category != ''\">and"
          + " g.category=#{q.category}</if> <if test=\"q.status != ''\">and"
          + " g.status=#{q.status}</if> <if test=\"q.condition != ''\">and"
          + " g.condition_level=#{q.condition}</if> <if test=\"q.minPrice != null\">and g.price"
          + " &gt;= #{q.minPrice}</if> <if test=\"q.maxPrice != null\">and g.price &lt;="
          + " #{q.maxPrice}</if>";
  String PAGE_ORDER =
      " <choose><when test=\"q.sort == 'priceAsc'\">order by g.price asc,g.id desc</when><when"
          + " test=\"q.sort == 'priceDesc'\">order by g.price desc,g.id desc</when><when"
          + " test=\"q.sort == 'popular'\">order by g.favorite_count desc,g.id"
          + " desc</when><otherwise>order by g.id desc</otherwise></choose>";

  @Select(
      "<script>select g.*,exists(select 1 from favorite where goods_id=g.id and user_id=#{userId})"
          + " favorite"
          + PAGE_WHERE
          + PAGE_ORDER
          + " limit #{q.size} offset #{q.offset}</script>")
  List<Goods> page(
      @Param("q") PageQuery q, @Param("userId") Long userId, @Param("scope") String scope);

  @Select("<script>select count(*)" + PAGE_WHERE + "</script>")
  long pageCount(
      @Param("q") PageQuery q, @Param("userId") Long userId, @Param("scope") String scope);

  @Select(
      "select g.*,exists(select 1 from favorite where goods_id=g.id and user_id=#{userId}) favorite"
          + " from goods g where id=#{id}")
  Goods findById(@Param("id") Long id, @Param("userId") Long userId);

  @Select("select * from goods where id=#{id} for update")
  Goods lock(Long id);

  @Insert(
      "insert into"
          + " goods(title,price,category,condition_level,location,description,image_url,seller_id,seller_name,status,favorite_count,ownership_verified,created_at)"
          + " values(#{title},#{price},#{category},#{conditionLevel},#{location},#{description},#{imageUrl},#{sellerId},#{sellerName},'ON_SALE',0,TRUE,CURRENT_TIMESTAMP)")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insert(Goods g);

  @Update(
      "update goods set"
          + " title=#{title},price=#{price},category=#{category},condition_level=#{conditionLevel},location=#{location},description=#{description},image_url=#{imageUrl},version=version+1"
          + " where id=#{id} and seller_id=#{sellerId} and ownership_verified=TRUE and status in"
          + " ('ON_SALE','OFF_SHELF')")
  int update(Goods g);

  @Update(
      "update goods set status=#{status},version=version+1 where id=#{id} and seller_id=#{sellerId}"
          + " and ownership_verified=TRUE and status in ('ON_SALE','OFF_SHELF') and"
          + " reserved_order_id is null")
  int updateStatus(
      @Param("id") Long id, @Param("sellerId") Long sellerId, @Param("status") String status);

  @Select("select count(*) from favorite where user_id=#{userId} and goods_id=#{id}")
  int favoriteExists(@Param("id") Long id, @Param("userId") Long userId);

  @Insert(
      "insert into favorite(user_id,goods_id,created_at) values(#{userId},#{id},CURRENT_TIMESTAMP)")
  int addFavorite(@Param("id") Long id, @Param("userId") Long userId);

  @Delete("delete from favorite where user_id=#{userId} and goods_id=#{id}")
  int removeFavorite(@Param("id") Long id, @Param("userId") Long userId);

  @Update("update goods set favorite_count=favorite_count+#{delta} where id=#{id}")
  int countFavorite(@Param("id") Long id, @Param("delta") int delta);

  @Update(
      "update goods set status='RESERVED',reserved_order_id=#{orderId},version=version+1 where"
          + " id=#{id} and status='ON_SALE' and ownership_verified=TRUE and reserved_order_id is"
          + " null")
  int reserve(@Param("id") Long id, @Param("orderId") Long orderId);

  @Update(
      "update goods set status=#{status},reserved_order_id=null,version=version+1 where id=#{id}"
          + " and status='RESERVED' and reserved_order_id=#{orderId}")
  int finish(@Param("id") Long id, @Param("orderId") Long orderId, @Param("status") String status);
}
