package com.recall.service.requirement;

import com.recall.dto.requirement.MeetingMinutesQuickCreateReq;
import com.recall.dto.requirement.MeetingMinutesSaveReq;
import com.recall.dto.requirement.RequirementDocumentCreateReq;
import com.recall.vo.requirement.RequirementDocumentVO;
import com.recall.vo.requirement.RequirementVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 需求文档 Service，管理 requirement_documents 表的数据访问。
 * <p>
 * 文档归属需求；新增/删除均校验需求存在且属于当前用户（越权统一 404）。
 *
 * @author recall
 */
public interface RequirementDocumentService {

    /**
     * 新增需求文档。
     *
     * @param requirementId 归属需求 ID
     * @param req           创建请求
     * @return 新建的文档
     */
    RequirementDocumentVO create(Long requirementId, RequirementDocumentCreateReq req);

    /**
     * 删除需求文档。
     *
     * @param documentId 文档 ID
     */
    void delete(Long documentId);

    /**
     * 查询指定需求下的全部文档（供 RequirementService 详情填充用）。
     *
     * @param requirementId 需求 ID
     * @return 文档 VO 列表
     */
    List<RequirementDocumentVO> listByRequirement(Long requirementId);

    /**
     * 删除指定需求下的全部文档（供删需求时级联调用）。
     *
     * @param requirementId 需求 ID
     */
    void deleteByRequirement(Long requirementId);

    // ===================== 会议纪要池（type=3 且未绑定需求） =====================

    /**
     * 查询当前用户未绑定需求的会议纪要（type=3 且 requirement_id IS NULL，会议日期倒序）。
     *
     * @return 纪要 VO 列表
     */
    List<RequirementDocumentVO> listUnboundMinutes();

    /**
     * 推送入库（系统态，userId 由推送配置指定，无用户上下文）。
     * <p>按 user+type=3+url 幂等：已存在则更新标题与会议日期（不动绑定关系），否则新建入池。
     *
     * @param userId      归属用户
     * @param req         标题/链接/会议日期
     * @param defaultDate 会议日期缺省值（推送日）
     * @return 落库后的纪要
     */
    RequirementDocumentVO upsertPushedMinute(Long userId, MeetingMinutesSaveReq req, LocalDate defaultDate);

    /**
     * 绑定纪要到需求（type=3 校验 + 需求归属校验）。
     *
     * @param documentId    纪要 ID
     * @param requirementId 目标需求 ID
     */
    void bindMinute(Long documentId, Long requirementId);

    /**
     * 解绑纪要回池（requirement_id 置 NULL）。
     *
     * @param documentId 纪要 ID
     */
    void unbindMinute(Long documentId);

    /**
     * 修改纪要标题（推送标题不准时手动纠正）。
     *
     * @param documentId 纪要 ID
     * @param title      新标题
     */
    void updateMinuteTitle(Long documentId, String title);

    /**
     * 快捷新建需求并绑定该纪要（原子，供「找不到合适需求」场景现场定义需求）。
     * <p>复用需求创建逻辑（标题唯一校验、分类归属校验），首提时间取当天；
     * 任一环节失败整批回滚。
     *
     * @param documentId 纪要 ID
     * @param req        新需求标题与分类
     * @return 新建的需求
     */
    RequirementVO createRequirementAndBindMinute(Long documentId, MeetingMinutesQuickCreateReq req);
}
