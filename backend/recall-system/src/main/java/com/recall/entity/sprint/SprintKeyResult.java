package com.recall.entity.sprint;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 冲刺-关键成果关联实体（多对多）。
 *
 * @author recall
 */
@Data
@TableName("sprint_key_results")
public class SprintKeyResult {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sprintId;

    private Long keyResultId;

    private LocalDateTime createdAt;
}
