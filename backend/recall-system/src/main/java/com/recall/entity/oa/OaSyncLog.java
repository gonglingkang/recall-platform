package com.recall.entity.oa;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.recall.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * OA 同步日志实体，一次同步一条。
 * <p>
 * status/step 用于前端轮询展示与失败定位；残留"运行中"超过阈值由同步服务判定为失败。
 *
 * @author recall
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_sync_logs")
public class OaSyncLog extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 平台周起始（周一） */
    private LocalDate weekStart;

    /** 触发方式: 1自动 2手动 */
    private Integer triggerType;

    /** 状态: 1运行中 2成功 3失败 */
    private Integer status;

    /** 执行到哪一步（失败定位用） */
    private String step;

    /** 失败原因 */
    private String errorMsg;

    /** OA 工时提交截止时间（同步时从表单读取，每周固定） */
    private LocalDateTime deadline;

    /** 同步时 OA 端该周工时是否已填报: false否 true是 */
    private Boolean oaSubmitted;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
