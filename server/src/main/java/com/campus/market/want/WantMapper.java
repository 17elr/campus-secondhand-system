package com.campus.market.want;

import org.apache.ibatis.annotations.*;
import java.util.List;
import com.campus.market.common.PageQuery;

@Mapper
public interface WantMapper {
    String PAGE_WHERE = " from want_post where (status='PUBLISHED' or user_id=#{userId}) <if test=\"q.onlyMine\">and user_id=#{userId}</if> <if test=\"q.status != ''\">and status=#{q.status}</if> <if test=\"q.category != ''\">and category=#{q.category}</if> <if test=\"q.keyword != ''\">and (title like concat('%',#{q.keyword},'%') or description like concat('%',#{q.keyword},'%') or requester_name like concat('%',#{q.keyword},'%'))</if>";

    @Select("<script>select *" + PAGE_WHERE + " order by id desc limit #{q.size} offset #{q.offset}</script>")
    List<Want> page(@Param("q") PageQuery query, @Param("userId") Long userId);

    @Select("<script>select count(*)" + PAGE_WHERE + "</script>")
    long pageCount(@Param("q") PageQuery query, @Param("userId") Long userId);

    @Select("select id,user_id userId,requester_name requesterName,title,category,budget,description,status,created_at createdAt from want_post where status='PUBLISHED' or user_id=#{userId} order by id desc")
    List<Want> findVisible(@Param("userId") Long userId);

    @Select("select id,user_id userId,requester_name requesterName,title,category,budget,description,status,created_at createdAt from want_post order by id desc")
    List<Want> findAll();

    @Insert("insert into want_post(user_id,requester_name,title,category,budget,description,status,created_at) values(#{userId},#{requesterName},#{title},#{category},#{budget},#{description},'PENDING',CURRENT_TIMESTAMP)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Want want);

    @Update("update want_post set status=#{status} where id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Select("select count(*) from want_post where status='PENDING'")
    int pendingCount();

    @Select("select * from want_post where id=#{id}")
    Want findById(Long id);

    @Update("update want_post set title=#{title},category=#{category},budget=#{budget},description=#{description},status='PENDING' where id=#{id} and user_id=#{userId} and status in ('PENDING','PUBLISHED')")
    int updateOwn(Want want);

    @Update("update want_post set status=#{status} where id=#{id} and user_id=#{userId} and (status='PUBLISHED' or (status='PENDING' and #{status}='WITHDRAWN'))")
    int updateOwnStatus(@Param("id") Long id, @Param("userId") Long userId, @Param("status") String status);
}
