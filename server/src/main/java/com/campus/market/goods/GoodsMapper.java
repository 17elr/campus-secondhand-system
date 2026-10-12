package com.campus.market.goods;

import org.apache.ibatis.annotations.*;
import java.util.List;
import com.campus.market.common.PageQuery;

@Mapper
public interface GoodsMapper {
    String PAGE_WHERE = " from goods g where 1=1 <choose><when test=\"scope == 'mine'\">and g.seller_name=(select nickname from sys_user where id=#{userId})</when><when test=\"scope == 'favorites'\">and exists(select 1 from favorite where goods_id=g.id and user_id=#{userId})</when><otherwise>and g.status='ON_SALE'</otherwise></choose> <if test=\"q.keyword != ''\">and (g.title like concat('%',#{q.keyword},'%') or g.description like concat('%',#{q.keyword},'%') or g.seller_name like concat('%',#{q.keyword},'%') or g.location like concat('%',#{q.keyword},'%'))</if> <if test=\"q.category != ''\">and g.category=#{q.category}</if> <if test=\"q.status != ''\">and g.status=#{q.status}</if> <if test=\"q.condition != ''\">and g.condition_level=#{q.condition}</if> <if test=\"q.minPrice != null\">and g.price &gt;= #{q.minPrice}</if> <if test=\"q.maxPrice != null\">and g.price &lt;= #{q.maxPrice}</if>";
    String PAGE_ORDER = " <choose><when test=\"q.sort == 'priceAsc'\">order by g.price asc,g.id desc</when><when test=\"q.sort == 'priceDesc'\">order by g.price desc,g.id desc</when><when test=\"q.sort == 'popular'\">order by g.favorite_count desc,g.id desc</when><otherwise>order by g.id desc</otherwise></choose>";

    @Select("<script>select g.*,exists(select 1 from favorite where goods_id=g.id and user_id=#{userId}) favorite"
            + PAGE_WHERE + PAGE_ORDER + " limit #{q.size} offset #{q.offset}</script>")
    List<Goods> page(@Param("q") PageQuery query, @Param("userId") Long userId, @Param("scope") String scope);

    @Select("<script>select count(*)" + PAGE_WHERE + "</script>")
    long pageCount(@Param("q") PageQuery query, @Param("userId") Long userId, @Param("scope") String scope);

    @Select("select g.id,g.title,g.price,g.category,g.condition_level conditionLevel,g.location,g.description,g.image_url imageUrl,g.seller_name sellerName,g.status,g.favorite_count favoriteCount,exists(select 1 from favorite f where f.goods_id=g.id and f.user_id=#{userId}) favorite,g.created_at createdAt from goods g where g.status='ON_SALE' and (#{keyword} is null or #{keyword}='' or g.title like concat('%',#{keyword},'%')) and (#{category} is null or #{category}='' or g.category=#{category}) order by g.id desc")
    List<Goods> findAll(@Param("keyword") String keyword, @Param("category") String category,
            @Param("userId") Long userId);

    @Select("select g.id,g.title,g.price,g.category,g.condition_level conditionLevel,g.location,g.description,g.image_url imageUrl,g.seller_name sellerName,g.status,g.favorite_count favoriteCount,exists(select 1 from favorite f where f.goods_id=g.id and f.user_id=#{userId}) favorite,g.created_at createdAt from goods g where g.id=#{id}")
    Goods findById(@Param("id") Long id, @Param("userId") Long userId);

    @Select("select g.id,g.title,g.price,g.category,g.condition_level conditionLevel,g.location,g.description,g.image_url imageUrl,g.seller_name sellerName,g.status,g.favorite_count favoriteCount,exists(select 1 from favorite f where f.goods_id=g.id and f.user_id=#{userId}) favorite,g.created_at createdAt from goods g where g.seller_name=(select nickname from sys_user where id=#{userId}) order by g.id desc")
    List<Goods> findMine(@Param("userId") Long userId);

    @Select("select g.id,g.title,g.price,g.category,g.condition_level conditionLevel,g.location,g.description,g.image_url imageUrl,g.seller_name sellerName,g.status,g.favorite_count favoriteCount,true favorite,g.created_at createdAt from goods g join favorite f on f.goods_id=g.id where f.user_id=#{userId} order by f.created_at desc")
    List<Goods> findFavorites(@Param("userId") Long userId);

    @Insert("insert into goods(title,price,category,condition_level,location,description,image_url,seller_name,status,favorite_count,created_at) values(#{title},#{price},#{category},#{conditionLevel},#{location},#{description},#{imageUrl},#{sellerName},'ON_SALE',0,CURRENT_TIMESTAMP)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Goods goods);

    @Update("update goods set title=#{title},price=#{price},category=#{category},condition_level=#{conditionLevel},location=#{location},description=#{description},image_url=#{imageUrl} where id=#{id} and seller_name=#{sellerName}")
    int update(Goods goods);

    @Update("update goods set status=#{status} where id=#{id} and seller_name=#{sellerName}")
    int updateStatus(@Param("id") Long id, @Param("sellerName") String sellerName,
            @Param("status") String status);

    @Select("select count(*) from favorite where user_id=#{userId} and goods_id=#{id}")
    int favoriteExists(@Param("id") Long id, @Param("userId") Long userId);

    @Insert("insert into favorite(user_id,goods_id,created_at) values(#{userId},#{id},CURRENT_TIMESTAMP)")
    int addFavorite(@Param("id") Long id, @Param("userId") Long userId);

    @Delete("delete from favorite where user_id=#{userId} and goods_id=#{id}")
    int removeFavorite(@Param("id") Long id, @Param("userId") Long userId);

    @Update("update goods set favorite_count=favorite_count+1 where id=#{id}")
    int increaseFavorite(Long id);

    @Update("update goods set favorite_count=greatest(favorite_count-1,0) where id=#{id}")
    int decreaseFavorite(Long id);

    @Update("update goods set status='RESERVED' where id=#{id} and status='ON_SALE'")
    int reserve(Long id);

    @Update("update goods set status='ON_SALE' where id=#{id} and status='RESERVED'")
    int release(Long id);
}
