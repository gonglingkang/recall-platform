package com.recall.dao.auth;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recall.entity.auth.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper（DAO 层）。
 *
 * @author recall
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
