package com.recall.entity.objectives;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.recall.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 关键成果成果记录 R 实体（v2.1），归属于关键成果 K。
 * <p>
 * K 切换到「已完成」时由用户提交，1:N，仅 content 文本。
 * 切回进行中/取消时保留 R；删 K 时级联物理删除 R。
 *
 * @author recall
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("key_result_records")
public class KeyResultRecord extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long keyResultId;

    private String content;
}
