package com.recall.entity.auth;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.recall.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户实体（PRD 9.1）。
 *
 * @author recall
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("users")
public class User extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名，全局唯一 */
    private String username;

    /** 邮箱，全局唯一 */
    private String email;

    /** 密码哈希(BCrypt) */
    private String passwordHash;

    /** 昵称 */
    private String nickname;

    /** 时区，如 Asia/Shanghai */
    private String timezone;

    /** 提醒总开关 */
    private Boolean reminderEnabled;
}
