package com.springleaf.knowseek.mapper;

import com.springleaf.knowseek.model.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {

    @Insert("INSERT INTO tb_user(user_id, email, password_hash, nickname, avatar_url, partner_id, status, notification_enabled, theme, language) " +
            "VALUES(#{userId}, #{email}, #{passwordHash}, #{nickname}, #{avatarUrl}, #{partnerId}, #{status}, #{notificationEnabled}, #{theme}, #{language})")
    int insert(User user);

    @Select("SELECT * FROM tb_user WHERE user_id = #{userId}")
    User selectById(String userId);


    @Update("UPDATE tb_user SET nickname = #{nickname}, avatar_url = #{avatarUrl}, theme = #{theme}, language = #{language}, " +
            "notification_enabled = #{notificationEnabled} WHERE user_id = #{userId}")
    int updateById(User user);


    @Update("UPDATE tb_user SET status = #{status} WHERE user_id = #{userId}")
    int updateStatus(@Param("userId") String userId, @Param("status") Integer status);

    @Delete("DELETE FROM tb_user WHERE user_id = #{userId}")
    int deleteById(String userId);

    @Select("SELECT * FROM tb_user WHERE status = 1 ORDER BY created_at DESC")
    List<User> selectAll();
}
