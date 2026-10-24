package com.campus.market.auth;

import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper {
  @Select("select * from sys_user where username=#{username}")
  User findByUsername(String username);

  @Select("select * from sys_user where id=#{id}")
  User findById(Long id);

  @Select("select * from sys_user where id=#{id} for update")
  User lock(Long id);

  @Insert(
      "insert into sys_user(username,password_hash,nickname,role,status,created_at)"
          + " values(#{username},#{passwordHash},#{nickname},#{role},1,CURRENT_TIMESTAMP)")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insert(User user);

  @Update("update sys_user set nickname=#{nickname} where id=#{id}")
  int profile(@Param("id") Long id, @Param("nickname") String nickname);

  @Update(
      "update sys_user set password_hash=#{hash},auth_version=auth_version+1 where id=#{id} and"
          + " password_hash=#{old}")
  int password(@Param("id") Long id, @Param("hash") String hash, @Param("old") String old);
}
