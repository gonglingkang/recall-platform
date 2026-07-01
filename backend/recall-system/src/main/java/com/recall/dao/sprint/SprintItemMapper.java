package com.recall.dao.sprint;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recall.entity.sprint.SprintItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 团队冲刺 Mapper（DAO 层）。
 *
 * @author recall
 */
@Mapper
public interface SprintItemMapper extends BaseMapper<SprintItem> {

    /**
     * 校验同用户同月冲刺任务标题是否已存在（标题唯一校验）。
     *
     * @param userId     用户 ID
     * @param month      月份 YYYY-MM
     * @param title      任务标题
     * @param excludeId  排除的任务 ID（编辑改名时传当前 id 排除自身，新增时传 null）
     * @return 是否存在重名
     */
    boolean existsByName(@Param("userId") Long userId,
                         @Param("month") String month,
                         @Param("title") String title,
                         @Param("excludeId") Long excludeId);
}
