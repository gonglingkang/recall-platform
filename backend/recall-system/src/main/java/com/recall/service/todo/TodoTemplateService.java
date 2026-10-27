package com.recall.service.todo;

import com.recall.dto.todo.TodoTemplateSaveReq;
import com.recall.vo.todo.TodoTemplateVO;

import java.util.List;

/**
 * 周期待办模板 Service。
 * <p>
 * 定时任务每天扫描启用的模板：门控（最近生成的待办仍待处理→跳过；待办已删视为已处理）
 * 满足且 next_date ≤ 今天时生成一条待办（planDate 语义=当天创建），并把 next_date
 * 推进到今天之后的下一个周期日——错过触发日（电脑关机）由启动补跑自然追平。
 *
 * @author recall
 */
public interface TodoTemplateService {

    /**
     * 查询当前用户的模板列表（含最近生成待办状态）。
     *
     * @return 模板列表
     */
    List<TodoTemplateVO> list();

    /**
     * 新增模板（next_date=今天之后的第一个周期日）。
     *
     * @param req 模板信息
     * @return 新建的模板
     */
    TodoTemplateVO create(TodoTemplateSaveReq req);

    /**
     * 编辑模板（重算 next_date=今天之后的第一个周期日）。
     *
     * @param id  模板 ID
     * @param req 模板信息
     * @return 更新后的模板
     */
    TodoTemplateVO update(Long id, TodoTemplateSaveReq req);

    /**
     * 删除模板（不动已生成的待办）。
     *
     * @param id 模板 ID
     */
    void delete(Long id);

    /**
     * 定时生成：扫描全部启用模板，完成门控 + next_date 到期则生成待办并推进 next_date。
     * <p>逻辑幂等（同一天重复执行第二次直接跳过），供每日定时与启动补跑共用。
     *
     * @return 本次生成的待办数
     */
    int generateDueTodos();
}
