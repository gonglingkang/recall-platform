package com.recall.service.objectives;

import com.recall.dto.objectives.ObjectiveImportParseReq;
import com.recall.dto.objectives.ObjectiveImportReq;
import com.recall.vo.objectives.ObjectiveImportPreviewVO;
import com.recall.vo.objectives.ObjectiveImportResultVO;

import java.util.List;

/**
 * 绩效目标导入 Service：公司绩效表内容 → 平台目标/关键成果。
 * <p>
 * 两步流程：parse 仅调 LLM 产出结构化预览（不写库，前端可编辑确认）；
 * importObjectives 按确认结果批量建目标与关键成果（同名冲突整批回滚）。
 *
 * @author recall
 */
public interface ObjectiveImportService {

    /**
     * 解析粘贴的绩效表格文本为结构化预览（LLM 结构化输出，不写库）。
     *
     * @param req 含粘贴文本
     * @return 目标预览列表（含关键成果）
     */
    List<ObjectiveImportPreviewVO> parse(ObjectiveImportParseReq req);

    /**
     * 批量导入目标与关键成果到指定月份。
     * <p>replace=true 时先清空该月全部已有目标（含其关键成果，不受单目标删除的
     * 已完成保护限制——该保护仅约束常规删除入口，覆盖导入须前端已向用户确认）。
     * <p>任一目标名与同月已有/本批其他目标重名抛 409，整批回滚。
     *
     * @param req 导入请求
     * @return 创建计数
     */
    ObjectiveImportResultVO importObjectives(ObjectiveImportReq req);
}
