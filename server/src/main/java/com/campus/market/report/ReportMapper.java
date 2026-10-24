package com.campus.market.report;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ReportMapper {
  @Insert(
      "insert into report(reporter_id,reporter_name,target_type,target_id,reason,status,created_at)"
          + " values(#{reporterId},#{reporterName},#{targetType},#{targetId},#{reason},'PENDING',CURRENT_TIMESTAMP)")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insert(Report report);

  @Select(
      "select id,reporter_name reporterName,target_type targetType,target_id"
          + " targetId,reason,status,created_at createdAt from report order by id desc")
  List<Map<String, Object>> findAll();

  @Update("update report set status=#{status} where id=#{id}")
  int updateStatus(@Param("id") Long id, @Param("status") String status);
}
