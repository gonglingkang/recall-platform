package com.recall.entity.oa;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.recall.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * OA 对接配置实体，每人一份（同 user_id 唯一）。
 * <p>
 * 密码只存 AES-GCM 密文，明文不出库、不回传前端。
 *
 * @author recall
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_user_config")
public class OaUserConfig extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** OA 根地址（不含末尾斜杠），如 https://erp.zoesoft.com.cn/seeyon */
    private String oaBaseUrl;

    /** OA 登录账号 */
    private String username;

    /** OA 登录密码密文（AES-GCM, Base64） */
    private String passwordCipher;

    /** 研发项目名（OA 项目组选择器匹配用），如 中台底座（三期） */
    private String devProjectName;

    /** 请假项目名（OA 项目组选择器匹配用） */
    private String leaveProjectName;

    /** 日报保存后自动同步: false关 true开 */
    private Boolean autoSync;
}
