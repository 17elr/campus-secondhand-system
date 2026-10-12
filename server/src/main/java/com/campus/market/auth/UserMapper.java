package com.campus.market.auth;

import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper {
    @Select("select id,username,password_hash passwordHash,nickname,role,status from sys_user where username=#{username}")
    User findByUsername(String username);

    @Select("select id,username,password_hash passwordHash,nickname,role,status from sys_user where id=#{id}")
    User findById(Long id);

    @Insert("insert into sys_user(username,password_hash,nickname,role,status,created_at) values(#{username},#{passwordHash},#{nickname},#{role},1,CURRENT_TIMESTAMP)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);
}
