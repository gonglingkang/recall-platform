package com.recall.service.requirement;

import com.recall.BaseTest;
import com.recall.common.api.PageResp;
import com.recall.common.api.ResultCode;
import com.recall.common.exception.BusinessException;
import com.recall.dao.objectives.KeyResultMapper;
import com.recall.dto.objectives.KeyResultCreateReq;
import com.recall.dto.objectives.KeyResultStatusReq;
import com.recall.dto.objectives.ObjectiveCreateReq;
import com.recall.dto.requirement.RequirementCategoryCreateReq;
import com.recall.dto.requirement.RequirementCreateReq;
import com.recall.dto.requirement.RequirementDocumentCreateReq;
import com.recall.dto.requirement.RequirementPageReq;
import com.recall.dto.requirement.RequirementStatusReq;
import com.recall.dto.requirement.RequirementUpdateReq;
import com.recall.entity.objectives.KeyResult;
import com.recall.enums.KeyResultStatus;
import com.recall.enums.RequirementDocumentType;
import com.recall.enums.RequirementStatus;
import com.recall.service.objectives.KeyResultService;
import com.recall.service.objectives.ObjectiveService;
import com.recall.vo.requirement.RequirementVO;
import com.recall.vo.requirement.RequirementDocumentVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 需求管理业务逻辑测试。
 * <p>
 * 重点：分页查询、状态流转、K 驱动联动、创建/编辑绑定 K、文档 CRUD、数据隔离。
 *
 * @author recall
 */
@Transactional
class RequirementServiceTest extends BaseTest {

    @Autowired
    private RequirementService requirementService;
    @Autowired
    private RequirementDocumentService requirementDocumentService;
    @Autowired
    private RequirementCategoryService requirementCategoryService;
    @Autowired
    private ObjectiveService objectiveService;
    @Autowired
    private KeyResultService keyResultService;
    @Autowired
    private KeyResultMapper keyResultMapper;

    // ===================== 基础 CRUD + 分页 =====================

    @Test
    void create_basic_statusDiscussing() {
        loginAsNewUser();
        RequirementVO vo = createReq("需求A");
        assertEquals(RequirementStatus.DISCUSSING.getValue(), vo.getStatus(), "新建默认讨论中");
        assertNull(vo.getKeyResultId(), "新建未绑K");
        assertTrue(vo.getDocuments().isEmpty(), "新建无文档");
    }

    @Test
    void create_firstDemandDate_canBackfillHistory() {
        loginAsNewUser();
        // 补录历史需求：首次需求时间早于今天
        LocalDate history = LocalDate.now().minusDays(10);
        RequirementVO vo = createReq("历史需求", history);
        assertEquals(history, vo.getFirstDemandDate(), "首次需求时间可补录历史日期");
        // 与 createdAt 区分：createdAt 是真实创建时间(今天附近)
        assertNotNull(vo.getCreatedAt());
    }

    @Test
    void create_duplicateTitle_shouldThrow() {
        loginAsNewUser();
        createReq("需求A");
        BusinessException ex = assertThrows(BusinessException.class, () -> createReq("需求A"));
        assertEquals(ResultCode.REQUIREMENT_TITLE_DUPLICATED.getCode(), ex.getCode());
    }

    @Test
    void update_renameToExisting_shouldThrow() {
        loginAsNewUser();
        createReq("需求A");
        Long bId = createReq("需求B").getId();
        RequirementUpdateReq upd = newUpdateReq("需求A", null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> requirementService.update(bId, upd));
        assertEquals(ResultCode.REQUIREMENT_TITLE_DUPLICATED.getCode(), ex.getCode());
    }

    @Test
    void page_filterByKeyword() {
        loginAsNewUser();
        createReq("登录改造");
        createReq("支付优化");
        createReq("登录修复");
        RequirementPageReq req = pageReq();
        req.setKeyword("登录");
        PageResp<RequirementVO> result = requirementService.page(req);
        assertEquals(2, result.getTotal(), "关键字'登录'匹配2条");
    }

    @Test
    void page_filterByStatus() {
        loginAsNewUser();
        Long aId = createReq("需求A").getId();
        changeStatus(aId, RequirementStatus.NOT_INVOLVED);
        createReq("需求B");
        RequirementPageReq req = pageReq();
        req.setStatuses(List.of(RequirementStatus.NOT_INVOLVED.getValue()));
        assertEquals(1, requirementService.page(req).getTotal(), "不涉及1条");
        req.setStatuses(List.of(RequirementStatus.DISCUSSING.getValue()));
        assertEquals(1, requirementService.page(req).getTotal(), "讨论中1条");
    }

    @Test
    void page_filterByMultipleStatuses() {
        loginAsNewUser();
        Long aId = createReq("需求A").getId();
        changeStatus(aId, RequirementStatus.NOT_INVOLVED);
        Long bId = createReq("需求B").getId();
        changeStatus(bId, RequirementStatus.IN_PROGRESS);
        createReq("需求C"); // 讨论中

        // 多状态筛选：不涉及 + 进行中 -> 命中2条
        RequirementPageReq req = pageReq();
        req.setStatuses(List.of(RequirementStatus.NOT_INVOLVED.getValue(),
                RequirementStatus.IN_PROGRESS.getValue()));
        assertEquals(2, requirementService.page(req).getTotal(), "不涉及+进行中=2条");
        // 多状态筛选：不涉及 + 讨论中 -> 命中2条
        req.setStatuses(List.of(RequirementStatus.NOT_INVOLVED.getValue(),
                RequirementStatus.DISCUSSING.getValue()));
        assertEquals(2, requirementService.page(req).getTotal(), "不涉及+讨论中=2条");
        // 三个状态 -> 命中3条
        req.setStatuses(List.of(RequirementStatus.NOT_INVOLVED.getValue(),
                RequirementStatus.IN_PROGRESS.getValue(),
                RequirementStatus.DISCUSSING.getValue()));
        assertEquals(3, requirementService.page(req).getTotal(), "不涉及+进行中+讨论中=3条");
    }

    @Test
    void page_filterByStatuses_emptyList_queriesAll() {
        loginAsNewUser();
        createReq("需求A");
        createReq("需求B");
        RequirementPageReq req = pageReq();
        req.setStatuses(Collections.emptyList());
        assertEquals(2, requirementService.page(req).getTotal(), "空状态列表不过滤");
    }

    @Test
    void page_pagination() {
        loginAsNewUser();
        for (int i = 0; i < 15; i++) {
            createReq("需求" + i);
        }
        RequirementPageReq req = pageReq();
        req.setPageSize(10);
        PageResp<RequirementVO> p1 = requirementService.page(req);
        assertEquals(15, p1.getTotal(), "总数15");
        assertEquals(10, p1.getRecords().size(), "首页10条");
        req.setPageNum(2);
        PageResp<RequirementVO> p2 = requirementService.page(req);
        assertEquals(5, p2.getRecords().size(), "次页5条");
    }

    @Test
    void page_filterByFirstDemandDateRange() {
        loginAsNewUser();
        // 三条需求，首次需求时间分别为 7/1、7/10、7/20
        createReq("需求1", LocalDate.of(2026, 7, 1));
        createReq("需求2", LocalDate.of(2026, 7, 10));
        createReq("需求3", LocalDate.of(2026, 7, 20));
        // 过滤 7/5 ~ 7/15 -> 只命中需求2
        RequirementPageReq req = pageReq();
        req.setStartDate(LocalDate.of(2026, 7, 5));
        req.setEndDate(LocalDate.of(2026, 7, 15));
        PageResp<RequirementVO> result = requirementService.page(req);
        assertEquals(1, result.getTotal(), "首次需求时间范围7/5-7/15只命中1条");
        assertEquals("需求2", result.getRecords().get(0).getTitle());
    }

    // ===================== 手动状态机（未绑K） =====================

    @Test
    void changeStatus_unbound_legalTransition() {
        loginAsNewUser();
        Long id = createReq("需求A").getId();
        // 讨论中 -> 进行中（合法）
        assertEquals(RequirementStatus.IN_PROGRESS.getValue(), changeStatus(id, RequirementStatus.IN_PROGRESS).getStatus());
        // 进行中 -> 开发完成（合法，填devCompleteDate）
        RequirementVO vo = changeStatus(id, RequirementStatus.DEV_DONE);
        assertEquals(RequirementStatus.DEV_DONE.getValue(), vo.getStatus());
        assertNotNull(vo.getDevCompleteDate(), "开发完成填devCompleteDate");
        LocalDate devDoneDate = vo.getDevCompleteDate();
        // 开发完成 -> 验收完成（合法，保留devCompleteDate）
        vo = changeStatus(id, RequirementStatus.ACCEPTANCE_DONE);
        assertEquals(RequirementStatus.ACCEPTANCE_DONE.getValue(), vo.getStatus());
        assertNotNull(vo.getAcceptanceDate());
        assertEquals(devDoneDate, vo.getDevCompleteDate(), "验收完成保留devCompleteDate");
        // 验收完成 -> 发布完成（合法，终态，保留devCompleteDate）
        vo = changeStatus(id, RequirementStatus.RELEASED);
        assertEquals(RequirementStatus.RELEASED.getValue(), vo.getStatus());
        assertNotNull(vo.getReleaseDate());
        assertEquals(devDoneDate, vo.getDevCompleteDate(), "发布完成保留devCompleteDate");
    }

    @Test
    void changeStatus_illegalTransition_shouldThrow() {
        loginAsNewUser();
        Long id = createReq("需求A").getId();
        // 讨论中 -> 开发完成（非法，需先进行中）
        BusinessException ex = assertThrows(BusinessException.class,
                () -> changeStatus(id, RequirementStatus.DEV_DONE));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void changeStatus_releasedTerminal_shouldThrow() {
        loginAsNewUser();
        Long id = createReq("需求A").getId();
        changeStatus(id, RequirementStatus.IN_PROGRESS);
        changeStatus(id, RequirementStatus.DEV_DONE);
        changeStatus(id, RequirementStatus.ACCEPTANCE_DONE);
        changeStatus(id, RequirementStatus.RELEASED);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> changeStatus(id, RequirementStatus.ACCEPTANCE_DONE));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void changeStatus_toNotInvolved_fillsCancelReason() {
        loginAsNewUser();
        Long id = createReq("需求A").getId();
        RequirementStatusReq req = new RequirementStatusReq();
        req.setStatus(RequirementStatus.NOT_INVOLVED);
        req.setCancelReason("不做");
        RequirementVO vo = requirementService.changeStatus(id, req);
        assertEquals(RequirementStatus.NOT_INVOLVED.getValue(), vo.getStatus());
        assertEquals("不做", vo.getCancelReason());
    }

    // ===================== 新状态机流转（未绑K） =====================

    @Test
    void changeStatus_notInvolved_toInProgress_legal() {
        loginAsNewUser();
        Long id = createReq("需求A").getId();
        changeStatus(id, RequirementStatus.NOT_INVOLVED);
        // 不涉及 -> 进行中（新增合法流转）
        RequirementVO vo = changeStatus(id, RequirementStatus.IN_PROGRESS);
        assertEquals(RequirementStatus.IN_PROGRESS.getValue(), vo.getStatus());
        assertNull(vo.getCancelReason(), "进入进行中清不涉及原因");
    }

    @Test
    void changeStatus_inProgress_toDiscussing_legal() {
        loginAsNewUser();
        Long id = createReq("需求A").getId();
        changeStatus(id, RequirementStatus.IN_PROGRESS);
        // 进行中 -> 讨论中（新增合法流转），清devCompleteDate
        RequirementVO vo = changeStatus(id, RequirementStatus.DISCUSSING);
        assertEquals(RequirementStatus.DISCUSSING.getValue(), vo.getStatus());
        assertNull(vo.getDevCompleteDate());
    }

    @Test
    void changeStatus_devDone_toInProgress_clearsDevCompleteDate() {
        loginAsNewUser();
        Long id = createReq("需求A").getId();
        changeStatus(id, RequirementStatus.IN_PROGRESS);
        changeStatus(id, RequirementStatus.DEV_DONE);
        // 开发完成 -> 进行中（新增合法流转），清devCompleteDate
        RequirementVO vo = changeStatus(id, RequirementStatus.IN_PROGRESS);
        assertEquals(RequirementStatus.IN_PROGRESS.getValue(), vo.getStatus());
        assertNull(vo.getDevCompleteDate(), "回退进行中清devCompleteDate");
    }

    @Test
    void changeStatus_devDone_toReleased_legal() {
        loginAsNewUser();
        Long id = createReq("需求A").getId();
        changeStatus(id, RequirementStatus.IN_PROGRESS);
        changeStatus(id, RequirementStatus.DEV_DONE);
        // 开发完成 -> 发布完成（新增合法流转，跳过验收）
        RequirementVO vo = changeStatus(id, RequirementStatus.RELEASED);
        assertEquals(RequirementStatus.RELEASED.getValue(), vo.getStatus());
        assertNotNull(vo.getReleaseDate());
        assertNotNull(vo.getDevCompleteDate(), "发布完成保留devCompleteDate");
    }

    @Test
    void changeStatus_acceptanceDone_cannotRevert_shouldThrow() {
        loginAsNewUser();
        Long id = createReq("需求A").getId();
        changeStatus(id, RequirementStatus.IN_PROGRESS);
        changeStatus(id, RequirementStatus.DEV_DONE);
        changeStatus(id, RequirementStatus.ACCEPTANCE_DONE);
        // 验收完成 -> 开发完成（现已非法，不可回退）
        BusinessException ex = assertThrows(BusinessException.class,
                () -> changeStatus(id, RequirementStatus.DEV_DONE));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
        // 验收完成 -> 进行中 / 讨论中（非法）
        assertThrows(BusinessException.class, () -> changeStatus(id, RequirementStatus.IN_PROGRESS));
        assertThrows(BusinessException.class, () -> changeStatus(id, RequirementStatus.DISCUSSING));
    }

    @Test
    void changeStatus_devDone_toDiscussing_illegal_shouldThrow() {
        loginAsNewUser();
        Long id = createReq("需求A").getId();
        changeStatus(id, RequirementStatus.IN_PROGRESS);
        changeStatus(id, RequirementStatus.DEV_DONE);
        // 开发完成 -> 讨论中（非法，需经进行中）
        BusinessException ex = assertThrows(BusinessException.class,
                () -> changeStatus(id, RequirementStatus.DISCUSSING));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    // ===================== 验收/发布入参 =====================

    @Test
    void changeStatus_toAcceptanceDone_withDateAndPerson_saved() {
        loginAsNewUser();
        Long id = createReq("需求A").getId();
        changeStatus(id, RequirementStatus.IN_PROGRESS);
        changeStatus(id, RequirementStatus.DEV_DONE);
        // 传入验收完成时间与验收人
        RequirementStatusReq req = new RequirementStatusReq();
        req.setStatus(RequirementStatus.ACCEPTANCE_DONE);
        req.setAcceptanceDate(LocalDate.of(2026, 7, 10));
        req.setAcceptancePerson("张三");
        RequirementVO vo = requirementService.changeStatus(id, req);
        assertEquals(RequirementStatus.ACCEPTANCE_DONE.getValue(), vo.getStatus());
        assertEquals(LocalDate.of(2026, 7, 10), vo.getAcceptanceDate(), "验收完成时间取入参");
        assertEquals("张三", vo.getAcceptancePerson(), "验收人取入参");
        assertNotNull(vo.getDevCompleteDate(), "验收完成保留devCompleteDate");
    }

    @Test
    void changeStatus_toAcceptanceDone_noDate_defaultsToday() {
        loginAsNewUser();
        Long id = createReq("需求A").getId();
        changeStatus(id, RequirementStatus.IN_PROGRESS);
        changeStatus(id, RequirementStatus.DEV_DONE);
        // 不传验收完成时间 -> 默认当天
        RequirementStatusReq req = new RequirementStatusReq();
        req.setStatus(RequirementStatus.ACCEPTANCE_DONE);
        RequirementVO vo = requirementService.changeStatus(id, req);
        assertEquals(LocalDate.now(), vo.getAcceptanceDate(), "不传验收时间默认当天");
        assertNull(vo.getAcceptancePerson(), "不传验收人为null");
    }

    @Test
    void changeStatus_toReleased_withDate_saved() {
        loginAsNewUser();
        Long id = createReq("需求A").getId();
        changeStatus(id, RequirementStatus.IN_PROGRESS);
        changeStatus(id, RequirementStatus.DEV_DONE);
        // 开发完成 -> 发布完成，传入发布时间
        RequirementStatusReq req = new RequirementStatusReq();
        req.setStatus(RequirementStatus.RELEASED);
        req.setReleaseDate(LocalDate.of(2026, 7, 20));
        RequirementVO vo = requirementService.changeStatus(id, req);
        assertEquals(RequirementStatus.RELEASED.getValue(), vo.getStatus());
        assertEquals(LocalDate.of(2026, 7, 20), vo.getReleaseDate(), "发布完成时间取入参");
        assertNotNull(vo.getDevCompleteDate(), "发布完成保留devCompleteDate");
    }

    @Test
    void changeStatus_toReleased_noDate_defaultsToday() {
        loginAsNewUser();
        Long id = createReq("需求A").getId();
        changeStatus(id, RequirementStatus.IN_PROGRESS);
        changeStatus(id, RequirementStatus.DEV_DONE);
        // 不传发布完成时间 -> 默认当天
        RequirementStatusReq req = new RequirementStatusReq();
        req.setStatus(RequirementStatus.RELEASED);
        RequirementVO vo = requirementService.changeStatus(id, req);
        assertEquals(LocalDate.now(), vo.getReleaseDate(), "不传发布时间默认当天");
    }

    // ===================== 创建/编辑绑定K + K驱动联动 =====================

    @Test
    void create_withKr_syncByKrStatus() {
        loginAsNewUser();
        Long krId = createKr("K1");
        // 创建时绑定K（K未开始 -> 讨论中）
        RequirementVO vo = createReqWithKr("需求A", krId);
        assertEquals(RequirementStatus.DISCUSSING.getValue(), vo.getStatus());
        assertEquals(krId, vo.getKeyResultId());
        assertNotNull(vo.getKeyResult(), "详情填充K摘要");
        assertEquals("K1", vo.getKeyResult().getName(), "K摘要仅含id/name/status");
    }

    @Test
    void create_withKrInProgress_reqInProgress() {
        loginAsNewUser();
        Long krId = createKr("K1");
        markKrInProgress(krId);
        RequirementVO vo = createReqWithKr("需求A", krId);
        assertEquals(RequirementStatus.IN_PROGRESS.getValue(), vo.getStatus(), "K进行中->需求进行中");
    }

    @Test
    void create_withKrDone_reqDevDone() {
        loginAsNewUser();
        Long krId = createKr("K1");
        markKrDone(krId);
        RequirementVO vo = createReqWithKr("需求A", krId);
        assertEquals(RequirementStatus.DEV_DONE.getValue(), vo.getStatus(), "K已完成->需求开发完成");
        assertNotNull(vo.getDevCompleteDate());
    }

    @Test
    void create_withHistoryDoneKr_devCompleteDateShouldBeKrCompleteDate() {
        loginAsNewUser();
        Long krId = createKr("K1");
        markKrDone(krId);
        // 模拟 K 在历史日期完成（completeDate 修正为 5 天前）
        LocalDate historyDone = LocalDate.now().minusDays(5);
        backfillKrCompleteDate(krId, historyDone);

        // 绑定这个早已完成的 K -> 需求开发完成时间应取 K 的完成时间，而非当天
        RequirementVO vo = createReqWithKr("需求A", krId);
        assertEquals(RequirementStatus.DEV_DONE.getValue(), vo.getStatus());
        assertEquals(historyDone, vo.getDevCompleteDate(),
                "绑定历史已完成的K，开发完成时间应取K的completeDate而非当天");
        assertNotEquals(LocalDate.now(), vo.getDevCompleteDate(), "不应是当天");
    }

    @Test
    void update_bindHistoryDoneKr_devCompleteDateShouldBeKrCompleteDate() {
        loginAsNewUser();
        Long krId = createKr("K1");
        markKrDone(krId);
        LocalDate historyDone = LocalDate.now().minusDays(7);
        backfillKrCompleteDate(krId, historyDone);

        Long reqId = createReq("需求A").getId();
        // 编辑绑定这个早已完成的 K
        RequirementVO vo = requirementService.update(reqId, newUpdateReq("需求A", krId));
        assertEquals(RequirementStatus.DEV_DONE.getValue(), vo.getStatus());
        assertEquals(historyDone, vo.getDevCompleteDate(),
                "编辑绑定历史已完成K，开发完成时间应取K的completeDate");
    }

    @Test
    void krChangeStatus_toDone_devCompleteDateShouldBeKrCompleteDate() {
        loginAsNewUser();
        Long krId = createKr("K1");
        Long reqId = createReqWithKr("需求A", krId).getId();
        markKrDone(krId);
        // K 刚切完成，completeDate 即当天，需求开发完成时间应与之相等
        KeyResult kr = keyResultService.getById(krId, false);
        RequirementVO vo = requirementService.getById(reqId);
        assertEquals(RequirementStatus.DEV_DONE.getValue(), vo.getStatus());
        assertEquals(kr.getCompleteDate(), vo.getDevCompleteDate(),
                "K切完成同步需求，开发完成时间应取K的completeDate");
    }

    @Test
    void update_bindKr_syncByKrStatus() {
        loginAsNewUser();
        Long krId = createKr("K1");
        Long reqId = createReq("需求A").getId();
        // 编辑时绑定K
        RequirementUpdateReq upd = newUpdateReq("需求A", krId);
        RequirementVO vo = requirementService.update(reqId, upd);
        assertEquals(krId, vo.getKeyResultId());
        assertEquals(RequirementStatus.DISCUSSING.getValue(), vo.getStatus(), "K未开始->讨论中");
    }

    @Test
    void update_rebindDifferentKr() {
        loginAsNewUser();
        Long kr1 = createKr("K1");
        Long kr2 = createKr("K2");
        Long reqId = createReqWithKr("需求A", kr1).getId();
        // 编辑改为绑 K2
        RequirementUpdateReq upd = newUpdateReq("需求A", kr2);
        RequirementVO vo = requirementService.update(reqId, upd);
        assertEquals(kr2, vo.getKeyResultId(), "改绑K2");
    }

    @Test
    void update_unbindKr_byNull() {
        loginAsNewUser();
        Long krId = createKr("K1");
        Long reqId = createReqWithKr("需求A", krId).getId();
        // 编辑时 keyResultId 传 null -> 解绑，回讨论中
        RequirementUpdateReq upd = newUpdateReq("需求A", null);
        RequirementVO vo = requirementService.update(reqId, upd);
        assertNull(vo.getKeyResultId(), "传null解绑K");
        assertEquals(RequirementStatus.DISCUSSING.getValue(), vo.getStatus(), "解绑回讨论中");
        assertNull(vo.getDevCompleteDate(), "解绑清devCompleteDate");
    }

    @Test
    void krChangeStatus_reqSynced() {
        loginAsNewUser();
        Long krId = createKr("K1");
        Long reqId = createReqWithKr("需求A", krId).getId();
        // K -> 进行中 -> 需求进行中
        markKrInProgress(krId);
        assertEquals(RequirementStatus.IN_PROGRESS.getValue(),
                requirementService.getById(reqId).getStatus());
        // K -> 已完成 -> 需求开发完成
        markKrDone(krId);
        RequirementVO vo = requirementService.getById(reqId);
        assertEquals(RequirementStatus.DEV_DONE.getValue(), vo.getStatus());
        assertNotNull(vo.getDevCompleteDate());
    }

    @Test
    void krReopen_reqBackToInProgress() {
        loginAsNewUser();
        Long krId = createKr("K1");
        Long reqId = createReqWithKr("需求A", krId).getId();
        markKrDone(krId);
        // K done -> in_progress（返工）-> 需求回进行中，清devCompleteDate
        markKrInProgress(krId);
        RequirementVO vo = requirementService.getById(reqId);
        assertEquals(RequirementStatus.IN_PROGRESS.getValue(), vo.getStatus());
        assertNull(vo.getDevCompleteDate(), "返工清devCompleteDate");
    }

    @Test
    void krCancelled_reqBackToDiscussing_unbind() {
        loginAsNewUser();
        Long krId = createKr("K1");
        Long reqId = createReqWithKr("需求A", krId).getId();
        markKrInProgress(krId);
        // K -> 取消 -> 需求回讨论中，解绑
        cancelKr(krId);
        RequirementVO vo = requirementService.getById(reqId);
        assertEquals(RequirementStatus.DISCUSSING.getValue(), vo.getStatus());
        assertNull(vo.getKeyResultId(), "K取消后解绑");
    }

    @Test
    void krDeleted_reqBackToDiscussing_unbind() {
        loginAsNewUser();
        Long krId = createKr("K1");
        Long reqId = createReqWithKr("需求A", krId).getId();
        markKrInProgress(krId);
        // 删 K -> 需求回讨论中，解绑
        keyResultService.delete(krId);
        RequirementVO vo = requirementService.getById(reqId);
        assertEquals(RequirementStatus.DISCUSSING.getValue(), vo.getStatus());
        assertNull(vo.getKeyResultId(), "K删除后解绑");
    }

    @Test
    void deleteObjective_reqUnbind() {
        loginAsNewUser();
        Long krId = createKr("K1");
        Long reqId = createReqWithKr("需求A", krId).getId();
        // 删 O 级联删 K -> 需求解绑回讨论中
        Long objId = objectiveService.list("2026-07").get(0).getId();
        objectiveService.delete(objId);
        RequirementVO vo = requirementService.getById(reqId);
        assertEquals(RequirementStatus.DISCUSSING.getValue(), vo.getStatus());
        assertNull(vo.getKeyResultId());
    }

    // ===================== 绑K约束 =====================

    @Test
    void create_withCancelledKr_shouldThrow() {
        loginAsNewUser();
        Long krId = createKr("K1");
        cancelKr(krId);
        BusinessException ex = assertThrows(BusinessException.class, () -> createReqWithKr("需求A", krId));
        assertEquals(ResultCode.REQUIREMENT_KR_CANCELLED.getCode(), ex.getCode());
    }

    @Test
    void bindKr_sharedByMultipleRequirements_ok() {
        loginAsNewUser();
        Long krId = createKr("K1");
        Long reqA = createReqWithKr("需求A", krId).getId();
        // 需求B 编辑绑同一K -> 允许（一个 K 可被多个需求绑定）
        Long reqB = createReq("需求B").getId();
        RequirementVO voB = requirementService.update(reqB, newUpdateReq("需求B", krId));
        assertEquals(krId, voB.getKeyResultId(), "需求B 也绑定了同一K");

        // 两个需求都关联了该 K
        assertEquals(krId, requirementService.getById(reqA).getKeyResultId());
        assertEquals(krId, requirementService.getById(reqB).getKeyResultId());
    }

    @Test
    void krChangeStatus_syncAllBoundRequirements() {
        loginAsNewUser();
        Long krId = createKr("K1");
        Long reqA = createReqWithKr("需求A", krId).getId();
        Long reqB = createReqWithKr("需求B", krId).getId();

        // K -> 进行中 -> 两个需求都同步为进行中
        markKrInProgress(krId);
        assertEquals(RequirementStatus.IN_PROGRESS.getValue(),
                requirementService.getById(reqA).getStatus(), "需求A同步为进行中");
        assertEquals(RequirementStatus.IN_PROGRESS.getValue(),
                requirementService.getById(reqB).getStatus(), "需求B同步为进行中");

        // K -> 已完成 -> 两个需求都同步为开发完成
        markKrDone(krId);
        assertEquals(RequirementStatus.DEV_DONE.getValue(),
                requirementService.getById(reqA).getStatus(), "需求A同步为开发完成");
        assertEquals(RequirementStatus.DEV_DONE.getValue(),
                requirementService.getById(reqB).getStatus(), "需求B同步为开发完成");
        assertNotNull(requirementService.getById(reqA).getDevCompleteDate());
        assertNotNull(requirementService.getById(reqB).getDevCompleteDate());
    }

    @Test
    void krCancelled_unbindsAllBoundRequirements() {
        loginAsNewUser();
        Long krId = createKr("K1");
        Long reqA = createReqWithKr("需求A", krId).getId();
        Long reqB = createReqWithKr("需求B", krId).getId();
        markKrInProgress(krId);

        // K 取消 -> 两个需求都解绑回讨论中
        cancelKr(krId);
        RequirementVO voA = requirementService.getById(reqA);
        RequirementVO voB = requirementService.getById(reqB);
        assertEquals(RequirementStatus.DISCUSSING.getValue(), voA.getStatus(), "需求A回讨论中");
        assertEquals(RequirementStatus.DISCUSSING.getValue(), voB.getStatus(), "需求B回讨论中");
        assertNull(voA.getKeyResultId(), "需求A解绑K");
        assertNull(voB.getKeyResultId(), "需求B解绑K");
    }

    @Test
    void krDeleted_unbindsAllBoundRequirements() {
        loginAsNewUser();
        Long krId = createKr("K1");
        Long reqA = createReqWithKr("需求A", krId).getId();
        Long reqB = createReqWithKr("需求B", krId).getId();

        // 删 K -> 两个需求都解绑回讨论中
        keyResultService.delete(krId);
        RequirementVO voA = requirementService.getById(reqA);
        RequirementVO voB = requirementService.getById(reqB);
        assertEquals(RequirementStatus.DISCUSSING.getValue(), voA.getStatus(), "需求A回讨论中");
        assertEquals(RequirementStatus.DISCUSSING.getValue(), voB.getStatus(), "需求B回讨论中");
        assertNull(voA.getKeyResultId(), "需求A解绑K");
        assertNull(voB.getKeyResultId(), "需求B解绑K");
    }

    @Test
    void krChangeStatus_detachedRequirementNotSynced() {
        loginAsNewUser();
        Long krId = createKr("K1");
        Long reqA = createReqWithKr("需求A", krId).getId();
        Long reqB = createReqWithKr("需求B", krId).getId();
        markKrDone(krId);
        // 需求B 验收完成（脱钩态，保留K关联）
        changeStatus(reqB, RequirementStatus.ACCEPTANCE_DONE);

        // K 返工 -> 需求A同步回进行中，需求B脱钩态不受影响
        markKrInProgress(krId);
        assertEquals(RequirementStatus.IN_PROGRESS.getValue(),
                requirementService.getById(reqA).getStatus(), "活跃态需求A同步为进行中");
        assertEquals(RequirementStatus.ACCEPTANCE_DONE.getValue(),
                requirementService.getById(reqB).getStatus(), "脱钩态需求B不受K驱动");
    }

    @Test
    void update_bindKr_notActiveState_shouldThrow() {
        loginAsNewUser();
        Long krId = createKr("K1");
        Long reqId = createReq("需求A").getId();
        // 进入不涉及（脱钩态）后编辑绑K -> 4704
        changeStatus(reqId, RequirementStatus.NOT_INVOLVED);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> requirementService.update(reqId, newUpdateReq("需求A", krId)));
        assertEquals(ResultCode.REQUIREMENT_KR_BIND_STATE_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    void changeStatus_boundKr_manualToActiveState_shouldThrow() {
        loginAsNewUser();
        Long krId = createKr("K1");
        Long reqId = createReqWithKr("需求A", krId).getId();
        // 绑K后手动改进行中（K活跃态）-> 禁止
        BusinessException ex = assertThrows(BusinessException.class,
                () -> changeStatus(reqId, RequirementStatus.IN_PROGRESS));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void changeStatus_boundKr_toAcceptance_keepKr() {
        loginAsNewUser();
        Long krId = createKr("K1");
        markKrDone(krId);
        Long reqId = createReqWithKr("需求A", krId).getId();
        // 需求=开发完成，手动验收 -> 验收完成，保留 K 关联（仅断开联动）
        RequirementVO vo = changeStatus(reqId, RequirementStatus.ACCEPTANCE_DONE);
        assertEquals(RequirementStatus.ACCEPTANCE_DONE.getValue(), vo.getStatus());
        assertEquals(krId, vo.getKeyResultId(), "验收完成保留K关联");
    }

    @Test
    void changeStatus_boundKr_toReleased_keepKr() {
        loginAsNewUser();
        Long krId = createKr("K1");
        markKrDone(krId);
        Long reqId = createReqWithKr("需求A", krId).getId();
        // 需求=开发完成，直接发布 -> 发布完成，保留 K 关联
        RequirementVO vo = changeStatus(reqId, RequirementStatus.RELEASED);
        assertEquals(RequirementStatus.RELEASED.getValue(), vo.getStatus());
        assertEquals(krId, vo.getKeyResultId(), "发布完成保留K关联");
    }

    @Test
    void changeStatus_toNotInvolved_unbindKr() {
        loginAsNewUser();
        Long krId = createKr("K1");
        markKrDone(krId);
        Long reqId = createReqWithKr("需求A", krId).getId();
        // 需求=开发完成，手动改不涉及 -> 解绑 K
        RequirementVO vo = changeStatus(reqId, RequirementStatus.NOT_INVOLVED);
        assertEquals(RequirementStatus.NOT_INVOLVED.getValue(), vo.getStatus());
        assertNull(vo.getKeyResultId(), "不涉及解绑K");
    }

    @Test
    void syncStatusByKeyResult_detachedState_keepKrAndNoSync() {
        loginAsNewUser();
        Long krId = createKr("K1");
        Long reqId = createReqWithKr("需求A", krId).getId();
        markKrDone(krId); // K完成 -> 需求开发完成
        changeStatus(reqId, RequirementStatus.ACCEPTANCE_DONE); // 验收完成（保留K关联）

        // 需求处于脱钩态时，K 状态变化不应再驱动需求
        markKrInProgress(krId); // K返工
        RequirementVO vo = requirementService.getById(reqId);
        assertEquals(RequirementStatus.ACCEPTANCE_DONE.getValue(), vo.getStatus(), "脱钩态不受K驱动");
        assertEquals(krId, vo.getKeyResultId(), "脱钩态仍保留K关联");
    }

    // ===================== 文档 CRUD =====================

    @Test
    void document_createAndDelete() {
        loginAsNewUser();
        Long reqId = createReq("需求A").getId();
        // 新增（三种类型，含文档时间）
        LocalDate docDate = LocalDate.of(2026, 7, 10);
        Long doc1 = createDoc(reqId, RequirementDocumentType.PROTOTYPE, "原型设计", "https://a.com/proto", docDate);
        Long doc2 = createDoc(reqId, RequirementDocumentType.REQUIREMENT, "需求文档", "https://a.com/req", LocalDate.now());
        Long doc3 = createDoc(reqId, RequirementDocumentType.MEETING, "会议纪要", "https://a.com/mt", LocalDate.now());
        // 详情含文档，验证文档时间与类型码
        RequirementVO vo = requirementService.getById(reqId);
        assertEquals(3, vo.getDocuments().size());
        RequirementDocumentVO proto = vo.getDocuments().stream()
                .filter(d -> d.getId().equals(doc1)).findFirst().orElseThrow();
        assertEquals("1", proto.getType(), "原型设计类型码为1");
        assertEquals(docDate, proto.getDocumentDate(), "文档时间正确");
        // 删除
        requirementDocumentService.delete(doc1);
        assertEquals(2, requirementDocumentService.listByRequirement(reqId).size());
    }

    @Test
    void deleteRequirement_cascadeDeleteDocuments() {
        loginAsNewUser();
        Long reqId = createReq("需求A").getId();
        createDoc(reqId, RequirementDocumentType.REQUIREMENT, "d1", "https://a.com", LocalDate.now());
        requirementService.delete(reqId);
        assertTrue(requirementDocumentService.listByRequirement(reqId).isEmpty(), "删需求连带删文档");
    }

    // ===================== 权限隔离 =====================

    @Test
    void dataIsolation_userCannotAccessOthers() {
        loginAsNewUser();
        Long reqId = createReq("私有需求").getId();
        loginAsNewUser();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> requirementService.getById(reqId));
        assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void create_withOtherUserKr_shouldThrow404() {
        loginAsNewUser();
        Long krId = createKr("K1");
        // 切换到另一用户，绑别人的K
        loginAsNewUser();
        BusinessException ex = assertThrows(BusinessException.class, () -> createReqWithKr("需求A", krId));
        assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode(), "绑别人的K越权404");
    }

    @Test
    void document_otherUserRequirement_shouldThrow404() {
        loginAsNewUser();
        Long reqId = createReq("私有需求").getId();
        loginAsNewUser();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> createDoc(reqId, RequirementDocumentType.REQUIREMENT, "d", "https://a.com", LocalDate.now()));
        assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
    }

    // ===================== 需求分类 =====================

    @Test
    void create_withoutCategory_shouldThrow() {
        loginAsNewUser();
        RequirementCreateReq req = new RequirementCreateReq();
        req.setTitle("需求A");
        req.setFirstDemandDate(LocalDate.now());
        // 不传 categoryId -> 校验失败（NotNull）
        BusinessException ex = assertThrows(BusinessException.class, () -> requirementService.create(req));
        assertEquals(ResultCode.PARAM_VALIDATE_FAILED.getCode(), ex.getCode());
    }

    @Test
    void create_withCategory_voCarriesCategoryName() {
        loginAsNewUser();
        Long mainId = requirementCategoryService.listTree().isEmpty()
                ? createMainCategory("主分类") : requirementCategoryService.listTree().get(0).getId();
        Long subId = createSubCategory(mainId, "子分类");

        RequirementCreateReq req = new RequirementCreateReq();
        req.setTitle("需求A");
        req.setFirstDemandDate(LocalDate.now());
        req.setCategoryId(mainId);
        req.setSubCategoryId(subId);
        RequirementVO vo = requirementService.create(req);
        assertEquals(mainId, vo.getCategoryId());
        assertEquals("主分类", vo.getCategoryName(), "VO 带主分类名称");
        assertEquals(subId, vo.getSubCategoryId());
        assertEquals("子分类", vo.getSubCategoryName(), "VO 带子分类名称");
    }

    @Test
    void create_subCategoryParentMismatch_shouldThrow() {
        loginAsNewUser();
        Long mainA = createMainCategory("主分类A");
        Long mainB = createMainCategory("主分类B");
        Long subB = createSubCategory(mainB, "主分类B的子分类");
        // mainB 的子分类绑到 mainA -> 4713
        RequirementCreateReq req = new RequirementCreateReq();
        req.setTitle("需求A");
        req.setFirstDemandDate(LocalDate.now());
        req.setCategoryId(mainA);
        req.setSubCategoryId(subB);
        BusinessException ex = assertThrows(BusinessException.class, () -> requirementService.create(req));
        assertEquals(ResultCode.REQUIREMENT_SUB_CATEGORY_PARENT_MISMATCH.getCode(), ex.getCode());
    }

    @Test
    void page_filterByCategory_includeSubs() {
        loginAsNewUser();
        Long mainId = createMainCategory("主分类");
        Long subId = createSubCategory(mainId, "子分类");
        // 1条挂主分类，1条挂子分类，1条挂其他主分类
        createReqWithCategory("需求1", mainId, null);
        createReqWithCategory("需求2", mainId, subId);
        Long otherMain = createMainCategory("其他主分类");
        createReqWithCategory("需求3", otherMain, null);

        // 按主分类筛选 -> 联动子分类，应命中需求1+需求2
        RequirementPageReq page = pageReq();
        page.setCategoryId(mainId);
        PageResp<RequirementVO> result = requirementService.page(page);
        assertEquals(2, result.getTotal(), "主分类筛选联动子分类");
    }

    @Test
    void page_filterBySubCategory_precise() {
        loginAsNewUser();
        Long mainId = createMainCategory("主分类");
        Long sub1 = createSubCategory(mainId, "子分类1");
        Long sub2 = createSubCategory(mainId, "子分类2");
        createReqWithCategory("需求1", mainId, sub1);
        createReqWithCategory("需求2", mainId, sub2);

        // 按子分类精确筛选 -> 只命中需求1
        RequirementPageReq page = pageReq();
        page.setSubCategoryId(sub1);
        PageResp<RequirementVO> result = requirementService.page(page);
        assertEquals(1, result.getTotal(), "子分类精确筛选");
        assertEquals("需求1", result.getRecords().get(0).getTitle());
    }

    private Long createMainCategory(String name) {
        RequirementCategoryCreateReq req = new RequirementCategoryCreateReq();
        req.setName(name);
        return requirementCategoryService.createCategory(req).getId();
    }

    private Long createSubCategory(Long parentId, String name) {
        RequirementCategoryCreateReq req = new RequirementCategoryCreateReq();
        req.setParentId(parentId);
        req.setName(name);
        return requirementCategoryService.createCategory(req).getId();
    }

    private void createReqWithCategory(String title, Long categoryId, Long subCategoryId) {
        RequirementCreateReq req = new RequirementCreateReq();
        req.setTitle(title);
        req.setFirstDemandDate(LocalDate.now());
        req.setCategoryId(categoryId);
        req.setSubCategoryId(subCategoryId);
        requirementService.create(req);
    }

    // ===================== 辅助 =====================

    /** 确保 当前用户 有一个默认主分类，返回其 ID（懒创建，已存在则复用） */
    private Long ensureDefaultCategory() {
        var tree = requirementCategoryService.listTree();
        if (!tree.isEmpty()) {
            return tree.get(0).getId();
        }
        RequirementCategoryCreateReq c = new RequirementCategoryCreateReq();
        c.setName("默认分类");
        return requirementCategoryService.createCategory(c).getId();
    }

    private RequirementVO createReq(String title) {
        return createReq(title, LocalDate.now());
    }

    private RequirementVO createReq(String title, LocalDate firstDemandDate) {
        RequirementCreateReq req = new RequirementCreateReq();
        req.setTitle(title);
        req.setFirstDemandDate(firstDemandDate);
        req.setCategoryId(ensureDefaultCategory());
        return requirementService.create(req);
    }

    private RequirementVO createReqWithKr(String title, Long krId) {
        RequirementCreateReq req = new RequirementCreateReq();
        req.setTitle(title);
        req.setFirstDemandDate(LocalDate.now());
        req.setCategoryId(ensureDefaultCategory());
        req.setKeyResultId(krId);
        return requirementService.create(req);
    }

    private RequirementUpdateReq newUpdateReq(String title, Long keyResultId) {
        RequirementUpdateReq req = new RequirementUpdateReq();
        req.setTitle(title);
        req.setFirstDemandDate(LocalDate.now());
        req.setCategoryId(ensureDefaultCategory());
        req.setKeyResultId(keyResultId);
        return req;
    }

    private RequirementPageReq pageReq() {
        RequirementPageReq req = new RequirementPageReq();
        req.setPageNum(1);
        req.setPageSize(10);
        return req;
    }

    private RequirementVO changeStatus(Long id, RequirementStatus target) {
        RequirementStatusReq req = new RequirementStatusReq();
        req.setStatus(target);
        return requirementService.changeStatus(id, req);
    }

    private Long createDoc(Long reqId, RequirementDocumentType type, String title, String url, LocalDate documentDate) {
        RequirementDocumentCreateReq c = new RequirementDocumentCreateReq();
        c.setType(type);
        c.setTitle(title);
        c.setUrl(url);
        c.setDocumentDate(documentDate);
        return requirementDocumentService.create(reqId, c).getId();
    }

    private Long createKr(String krName) {
        ObjectiveCreateReq objReq = new ObjectiveCreateReq();
        objReq.setMonth("2026-07");
        objReq.setName("目标" + unique());
        Long objId = objectiveService.create(objReq).getId();
        KeyResultCreateReq krReq = new KeyResultCreateReq();
        krReq.setObjectiveId(objId);
        krReq.setName(krName);
        return keyResultService.create(krReq).getId();
    }

    private void markKrDone(Long krId) {
        KeyResultStatusReq done = new KeyResultStatusReq();
        done.setStatus(KeyResultStatus.DONE);
        keyResultService.changeStatus(krId, done);
    }

    private void markKrInProgress(Long krId) {
        KeyResultStatusReq inProgress = new KeyResultStatusReq();
        inProgress.setStatus(KeyResultStatus.IN_PROGRESS);
        keyResultService.changeStatus(krId, inProgress);
    }

    private void cancelKr(Long krId) {
        KeyResultStatusReq cancel = new KeyResultStatusReq();
        cancel.setStatus(KeyResultStatus.CANCELLED);
        cancel.setCancelReason("不需要了");
        keyResultService.changeStatus(krId, cancel);
    }

    /** 测试夹具：直接修正 K 的 completeDate 为指定历史日期，模拟"早已完成的K" */
    private void backfillKrCompleteDate(Long krId, LocalDate date) {
        KeyResult kr = new KeyResult();
        kr.setId(krId);
        kr.setCompleteDate(date);
        keyResultMapper.updateById(kr);
    }
}
