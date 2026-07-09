package com.recall.service.objectives;

import com.recall.BaseTest;
import com.recall.common.api.ResultCode;
import com.recall.common.exception.BusinessException;
import com.recall.dto.objectives.KeyResultCreateReq;
import com.recall.dto.objectives.KeyResultStatusReq;
import com.recall.dto.objectives.KeyResultUpdateReq;
import com.recall.dto.objectives.ObjectiveCreateReq;
import com.recall.dto.objectives.ObjectiveUpdateReq;
import com.recall.dto.sprint.SprintCreateReq;
import com.recall.dto.sprint.SprintInvolvedReq;
import com.recall.dto.sprint.SprintLinkReq;
import com.recall.enums.KeyResultStatus;
import com.recall.service.sprint.SprintService;
import com.recall.vo.objectives.KeyResultVO;
import com.recall.vo.objectives.ObjectiveVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 月度绩效目标 Service 测试（v2.0）。
 * <p>
 * O 的逻辑测 ObjectiveService；K 的逻辑测 KeyResultService；二者协作验证派生计算。
 * 重点：O 的派生计算（progress/status/完成时间）、连带删K、K状态切换维护 completeDate、数据隔离。
 *
 * @author recall
 */
@Transactional
class ObjectiveServiceTest extends BaseTest {

    @Autowired
    private ObjectiveService objectiveService;
    @Autowired
    private KeyResultService keyResultService;
    @Autowired
    private SprintService sprintService;
    @Autowired
    private KeyResultRecordService keyResultRecordService;

    @Test
    void create_shouldDefaultProgressZeroAndNotStarted() {
        loginAsNewUser();
        ObjectiveVO vo = objectiveService.create(objReq("2026-07", "目标"));
        assertEquals(0, vo.getProgress(), "无K时进度=0");
        assertEquals("0", vo.getStatus());
        assertNull(vo.getPlanCompleteDate());
        assertNull(vo.getActualCompleteDate());
        assertTrue(vo.getKeyResults().isEmpty());
    }

    @Test
    void list_shouldFilterByMonth() {
        loginAsNewUser();
        objectiveService.create(objReq("2026-07", "本月目标"));
        objectiveService.create(objReq("2026-06", "上月目标"));

        assertEquals(1, objectiveService.list("2026-07").size(), "按月份隔离");
        assertEquals(1, objectiveService.list("2026-06").size());
    }

    @Test
    void derivedProgress_shouldBeDoneCountOverTotal() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "目标")).getId();
        Long k1 = keyResultService.create(krCreate(objId, "K1")).getId();
        Long k2 = keyResultService.create(krCreate(objId, "K2")).getId();
        keyResultService.create(krCreate(objId, "K3")); // 未完成

        markDone(k1);
        markDone(k2);

        List<ObjectiveVO> list = objectiveService.list("2026-07");
        ObjectiveVO vo = list.get(0);
        assertEquals(3, vo.getKeyResults().size());
        assertEquals(66, vo.getProgress(), "2/3 完成 → 66%");
        assertEquals("1", vo.getStatus());
        assertNull(vo.getActualCompleteDate(), "未全完成 → 实际完成时间 null");
    }

    @Test
    void derivedStatus_allDone_shouldBeDoneWithActualCompleteDate() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "目标")).getId();
        Long k1 = keyResultService.create(krCreateWithPlan(objId, "K1", "2026-07-15")).getId();
        Long k2 = keyResultService.create(krCreateWithPlan(objId, "K2", "2026-07-31")).getId();

        markDone(k1);
        markDone(k2);

        ObjectiveVO vo = objectiveService.list("2026-07").get(0);
        assertEquals(100, vo.getProgress());
        assertEquals("2", vo.getStatus());
        assertEquals(LocalDate.of(2026, 7, 31), vo.getPlanCompleteDate(), "计划完成时间=max(K计划)");
        assertNotNull(vo.getActualCompleteDate(), "全完成 → 有实际完成时间");
    }

    @Test
    void derivedStatus_allNotStarted_shouldBeNotStarted() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "目标")).getId();
        keyResultService.create(krCreate(objId, "K1"));
        keyResultService.create(krCreate(objId, "K2"));

        ObjectiveVO vo = objectiveService.list("2026-07").get(0);
        assertEquals(0, vo.getProgress());
        assertEquals("0", vo.getStatus());
    }

    @Test
    void changeStatus_doneThenUndo_shouldManageCompleteDate() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "目标")).getId();
        Long k1 = keyResultService.create(krCreate(objId, "K1")).getId();

        // →done：completeDate 填当天
        KeyResultVO done = markDone(k1);
        assertEquals("2", done.getStatus());
        assertNotNull(done.getCompleteDate());

        // →in_progress：completeDate 清空
        KeyResultStatusReq inProgress = new KeyResultStatusReq();
        inProgress.setStatus(KeyResultStatus.IN_PROGRESS);
        KeyResultVO undone = keyResultService.changeStatus(k1, inProgress);
        assertEquals("1", undone.getStatus(), "切回进行中");
        assertNull(undone.getCompleteDate(), "切回非done应清空完成时间");
    }

    @Test
    void statusMachine_legalTransitions() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "目标")).getId();
        Long k1 = keyResultService.create(krCreate(objId, "K1")).getId();

        // 未开始 → 进行中（开始）
        assertEquals("1", markInProgress(k1).getStatus());
        // 进行中 → 已完成（完成）
        assertEquals("2", markDone(k1).getStatus());
        // 已完成 → 进行中（取消完成）
        assertEquals("1", markInProgress(k1).getStatus());
    }

    @Test
    void statusMachine_notStartedToDone_shouldBeLegal() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "目标")).getId();
        Long k1 = keyResultService.create(krCreate(objId, "K1")).getId();

        // 未开始 → 已完成（跳级完成）
        KeyResultVO done = markDone(k1);
        assertEquals("2", done.getStatus());
        assertNotNull(done.getCompleteDate(), "跳级完成也应填完成时间");
    }

    @Test
    void statusMachine_illegalTransition_inProgressToNotStarted_shouldThrow() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "目标")).getId();
        Long k1 = keyResultService.create(krCreate(objId, "K1")).getId();
        markInProgress(k1); // 进行中

        // 进行中 → 未开始：非法
        KeyResultStatusReq toNotStarted = new KeyResultStatusReq();
        toNotStarted.setStatus(KeyResultStatus.NOT_STARTED);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> keyResultService.changeStatus(k1, toNotStarted));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void statusMachine_illegalTransition_doneToNotStarted_shouldThrow() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "目标")).getId();
        Long k1 = keyResultService.create(krCreate(objId, "K1")).getId();
        markDone(k1); // 已完成

        // 已完成 → 未开始：非法
        KeyResultStatusReq toNotStarted = new KeyResultStatusReq();
        toNotStarted.setStatus(KeyResultStatus.NOT_STARTED);
        assertThrows(BusinessException.class, () -> keyResultService.changeStatus(k1, toNotStarted));
    }

    @Test
    void deleteObjective_shouldCascadeDeleteKeyResults() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "目标")).getId();
        Long k1 = keyResultService.create(krCreate(objId, "K1")).getId();
        keyResultService.create(krCreate(objId, "K2"));

        objectiveService.delete(objId);

        // O 已删 → 查询返回空
        assertTrue(objectiveService.list("2026-07").isEmpty());
        // K 也应访问不到（越权/不存在 → 404）
        assertThrows(BusinessException.class, () -> keyResultService.update(k1, krUpdate("x")));
    }

    @Test
    void deleteObjective_hasDoneKeyResult_shouldThrow() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "目标")).getId();
        Long k1 = keyResultService.create(krCreate(objId, "K1")).getId();
        keyResultService.create(krCreate(objId, "K2"));
        markDone(k1); // K1 已完成

        BusinessException ex = assertThrows(BusinessException.class,
                () -> objectiveService.delete(objId));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());

        // 删除被拒 → O 仍在
        assertEquals(1, objectiveService.list("2026-07").size());
    }

    @Test
    void deleteObjective_allNotStartedKeyResults_shouldOk() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "目标")).getId();
        keyResultService.create(krCreate(objId, "K1"));
        keyResultService.create(krCreate(objId, "K2"));

        objectiveService.delete(objId);
        assertTrue(objectiveService.list("2026-07").isEmpty());
    }

    @Test
    void deleteKeyResult_shouldRecomputeObjectiveProgress() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "目标")).getId();
        Long k1 = keyResultService.create(krCreate(objId, "K1")).getId();
        keyResultService.create(krCreate(objId, "K2"));
        markDone(k1); // 1/2 = 50%

        assertEquals(50, objectiveService.list("2026-07").get(0).getProgress());

        // 删除已完成的 K1 → 剩1个未完成 → 0%
        keyResultService.delete(k1);
        ObjectiveVO vo = objectiveService.list("2026-07").get(0);
        assertEquals(1, vo.getKeyResults().size());
        assertEquals(0, vo.getProgress(), "删除K后O进度自动重算");
    }

    @Test
    void dataIsolation_userCannotAccessOthersObjective() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "私有目标")).getId();

        loginAsNewUser(); // 切到另一个用户
        BusinessException ex = assertThrows(BusinessException.class,
                () -> objectiveService.update(objId, updReq("篡改")));
        assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void dataIsolation_userCannotAccessOthersKeyResult() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "私有目标")).getId();
        Long k1 = keyResultService.create(krCreate(objId, "K1")).getId();

        loginAsNewUser(); // 切到另一个用户
        BusinessException ex = assertThrows(BusinessException.class,
                () -> keyResultService.update(k1, krUpdate("篡改")));
        assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void createKeyResult_objectiveNotOwned_should404() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "我的目标")).getId();

        loginAsNewUser(); // 切到另一个用户，试图在别人的目标下建K
        BusinessException ex = assertThrows(BusinessException.class,
                () -> keyResultService.create(krCreate(objId, "K")));
        assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void updateObjective_shouldNotChangeDerivedFields() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "目标")).getId();
        Long k1 = keyResultService.create(krCreate(objId, "K1")).getId();
        markDone(k1);

        // 编辑 O 名称
        ObjectiveUpdateReq upd = new ObjectiveUpdateReq();
        upd.setName("新名称");
        ObjectiveVO vo = objectiveService.update(objId, upd);
        assertEquals("新名称", vo.getName());
        assertEquals(100, vo.getProgress(), "编辑后派生进度仍正确");
    }

    @Test
    void create_duplicateNameSameMonth_shouldThrow() {
        loginAsNewUser();
        objectiveService.create(objReq("2026-07", "学习提升"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> objectiveService.create(objReq("2026-07", "学习提升")));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void create_duplicateNameDifferentMonth_ok() {
        loginAsNewUser();
        objectiveService.create(objReq("2026-07", "学习提升"));

        // 跨月重名允许（同用户不同月）
        ObjectiveVO vo = objectiveService.create(objReq("2026-08", "学习提升"));
        assertEquals("2026-08", vo.getMonth());
        assertEquals("学习提升", vo.getName());
    }

    @Test
    void update_renameToExistingSameMonth_shouldThrow() {
        loginAsNewUser();
        objectiveService.create(objReq("2026-07", "目标A"));
        Long objB = objectiveService.create(objReq("2026-07", "目标B")).getId();

        // 把 B 改名成 A → 同月冲突
        ObjectiveUpdateReq rename = new ObjectiveUpdateReq();
        rename.setName("目标A");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> objectiveService.update(objB, rename));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void list_returnsSprintIdsOnKeyResults() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "目标")).getId();
        Long krId = keyResultService.create(krCreate(objId, "K1")).getId();

        // 创建冲刺并标记需介入，再关联 K
        SprintCreateReq sp = new SprintCreateReq();
        sp.setMonth("2026-07");
        sp.setTitle("冲刺1");
        Long sprintId = sprintService.create(sp).getId();
        SprintInvolvedReq involved = new SprintInvolvedReq();
        involved.setNeedInvolved(true);
        sprintService.toggleInvolved(sprintId, involved);
        SprintLinkReq link = new SprintLinkReq();
        link.setKeyResultIds(List.of(krId));
        sprintService.linkKeyResults(sprintId, link);

        // 绩效列表的 K 应返回关联的冲刺 ID
        ObjectiveVO vo = objectiveService.list("2026-07").get(0);
        KeyResultVO krVO = vo.getKeyResults().get(0);
        assertEquals(1, krVO.getSprintIds().size(), "K 应返回 1 个关联冲刺");
        assertEquals(sprintId, krVO.getSprintIds().get(0), "关联冲刺 ID 应匹配");
    }

    @Test
    void invalidMonth_shouldThrow() {
        loginAsNewUser();
        assertThrows(BusinessException.class, () -> objectiveService.list("2026/07"));
    }

    @Test
    void createKeyResult_duplicateNameSameObjective_shouldThrow() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "目标")).getId();
        keyResultService.create(krCreate(objId, "K1"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> keyResultService.create(krCreate(objId, "K1")));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void createKeyResult_duplicateNameDifferentObjective_ok() {
        loginAsNewUser();
        Long objA = objectiveService.create(objReq("2026-07", "目标A")).getId();
        Long objB = objectiveService.create(objReq("2026-07", "目标B")).getId();
        keyResultService.create(krCreate(objA, "K1"));

        // 不同目标下重名允许
        KeyResultVO vo = keyResultService.create(krCreate(objB, "K1"));
        assertEquals("K1", vo.getName());
    }

    @Test
    void updateKeyResult_renameToExistingSameObjective_shouldThrow() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "目标")).getId();
        keyResultService.create(krCreate(objId, "K1"));
        Long k2 = keyResultService.create(krCreate(objId, "K2")).getId();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> keyResultService.update(k2, krUpdate("K1")));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void updateKeyResult_doneCannotChangePlanCompleteDate_shouldThrow() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "目标")).getId();
        Long k1 = keyResultService.create(krCreate(objId, "K1")).getId();
        markDone(k1);

        KeyResultUpdateReq upd = new KeyResultUpdateReq();
        upd.setPlanCompleteDate(LocalDate.of(2026, 8, 1));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> keyResultService.update(k1, upd));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void updateKeyResult_doneCanEditNameAndDescription_ok() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "目标")).getId();
        Long k1 = keyResultService.create(krCreate(objId, "K1")).getId();
        markDone(k1);

        KeyResultUpdateReq upd = new KeyResultUpdateReq();
        upd.setName("K1-改名");
        upd.setDescription("已完成但可改描述");
        KeyResultVO vo = keyResultService.update(k1, upd);
        assertEquals("K1-改名", vo.getName());
        assertEquals("已完成但可改描述", vo.getDescription());
        assertEquals("2", vo.getStatus(), "状态未变");
    }

    @Test
    void createKeyResult_done_planCompleteDateDefaultsToToday() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "目标")).getId();
        // 已完成且未传计划完成时间 → 默认当天
        KeyResultCreateReq req = krCreate(objId, "K1");
        req.setStatus(KeyResultStatus.DONE);
        KeyResultVO vo = keyResultService.create(req);

        assertEquals(LocalDate.now(), vo.getPlanCompleteDate(), "已完成未传计划时间 → 默认当天");
        assertEquals(LocalDate.now(), vo.getCompleteDate(), "已完成 → completeDate 填当天");
    }

    @Test
    void createKeyResult_doneWithUserPlan_respected() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "目标")).getId();
        // 已完成但用户传了计划完成时间 → 用用户的，不被当天覆盖
        KeyResultCreateReq req = krCreateWithPlan(objId, "K1", "2026-07-15");
        req.setStatus(KeyResultStatus.DONE);
        KeyResultVO vo = keyResultService.create(req);

        assertEquals(LocalDate.of(2026, 7, 15), vo.getPlanCompleteDate(), "用户传了计划时间 → 用用户的");
        assertEquals(LocalDate.now(), vo.getCompleteDate(), "已完成 → completeDate 仍填当天");
    }

    @Test
    void createKeyResult_notDoneWithoutPlan_defaultsToEndOfMonth() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "目标")).getId();
        // 未传 status（默认未开始）且未传计划完成时间 → 月底
        KeyResultVO vo = keyResultService.create(krCreate(objId, "K1"));
        assertEquals(LocalDate.of(2026, 7, 31), vo.getPlanCompleteDate(), "未传计划时间 → 目标月月底");
    }

    @Test
    void createKeyResult_inProgressWithoutPlan_defaultsToEndOfMonth() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-02", "目标")).getId();
        KeyResultCreateReq req = krCreate(objId, "K1");
        req.setStatus(KeyResultStatus.IN_PROGRESS);
        KeyResultVO vo = keyResultService.create(req);
        assertEquals(LocalDate.of(2026, 2, 28), vo.getPlanCompleteDate(), "进行中且未传计划时间 → 月底（闰年校验2月）");
    }

    @Test
    void createKeyResult_userPlanProvided_respected() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "目标")).getId();
        // 用户传了计划时间 → 用用户的，不覆盖为月底
        KeyResultVO vo = keyResultService.create(krCreateWithPlan(objId, "K1", "2026-07-15"));
        assertEquals(LocalDate.of(2026, 7, 15), vo.getPlanCompleteDate());
    }

    // ===================== 取消状态 =====================

    @Test
    void changeStatus_cancelFromNotStarted_ok() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "目标")).getId();
        Long k1 = keyResultService.create(krCreate(objId, "K1")).getId();

        KeyResultVO vo = cancel(k1, "需求未定");
        assertEquals("3", vo.getStatus(), "未开始 → 已取消");
        assertEquals("需求未定", vo.getCancelReason(), "取消原因已存入");
        assertNull(vo.getCompleteDate(), "取消时清空完成时间");
    }

    @Test
    void changeStatus_cancelFromInProgress_ok() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "目标")).getId();
        Long k1 = keyResultService.create(krCreate(objId, "K1")).getId();
        markInProgress(k1);

        KeyResultVO vo = cancel(k1, "方案变更");
        assertEquals("3", vo.getStatus(), "进行中 → 已取消");
        assertEquals("方案变更", vo.getCancelReason());
    }

    @Test
    void changeStatus_cancelFromDone_shouldThrow() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "目标")).getId();
        Long k1 = keyResultService.create(krCreate(objId, "K1")).getId();
        markDone(k1);

        BusinessException ex = assertThrows(BusinessException.class, () -> cancel(k1, "不想做了"));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode(), "已完成不可直接取消");
    }

    @Test
    void changeStatus_restoreFromCancelled_ok() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "目标")).getId();
        Long k1 = keyResultService.create(krCreate(objId, "K1")).getId();
        cancel(k1, "需求未定");

        // 恢复 → 未开始，取消原因清空
        KeyResultStatusReq restore = new KeyResultStatusReq();
        restore.setStatus(KeyResultStatus.NOT_STARTED);
        KeyResultVO vo = keyResultService.changeStatus(k1, restore);
        assertEquals("0", vo.getStatus(), "已取消 → 未开始");
        assertNull(vo.getCancelReason(), "恢复时清空取消原因");
    }

    @Test
    void changeStatus_cancelIllegalRestoreToInProgress_shouldThrow() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "目标")).getId();
        Long k1 = keyResultService.create(krCreate(objId, "K1")).getId();
        cancel(k1, "需求未定");

        KeyResultStatusReq toInProgress = new KeyResultStatusReq();
        toInProgress.setStatus(KeyResultStatus.IN_PROGRESS);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> keyResultService.changeStatus(k1, toInProgress));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode(), "已取消只能恢复为未开始");
    }

    @Test
    void derivedProgress_shouldExcludeCancelled() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "目标")).getId();
        Long k1 = keyResultService.create(krCreate(objId, "K1")).getId();
        Long k2 = keyResultService.create(krCreate(objId, "K2")).getId();
        keyResultService.create(krCreate(objId, "K3")); // 未开始
        Long k4 = keyResultService.create(krCreate(objId, "K4")).getId();
        markDone(k1);
        markDone(k2);
        cancel(k4, "需求取消"); // K4 取消，不计入分母

        ObjectiveVO vo = objectiveService.list("2026-07").get(0);
        assertEquals(66, vo.getProgress(), "2/3 有效K完成 → 66%（取消的不计入分母）");
        assertEquals(1, vo.getCancelledCount(), "取消数=1");
        assertEquals("1", vo.getStatus(), "仍有未完成 → 进行中");
    }

    @Test
    void derivedStatus_allCancelled_shouldBeCancelled() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "目标")).getId();
        Long k1 = keyResultService.create(krCreate(objId, "K1")).getId();
        Long k2 = keyResultService.create(krCreate(objId, "K2")).getId();
        cancel(k1, "需求1取消");
        cancel(k2, "需求2取消");

        ObjectiveVO vo = objectiveService.list("2026-07").get(0);
        assertEquals("3", vo.getStatus(), "所有K取消 → O 已取消");
        assertEquals(0, vo.getProgress(), "无有效K → 进度0");
        assertEquals(2, vo.getCancelledCount(), "取消数=2");
        assertEquals(2, vo.getKeyResults().size(), "取消的K仍返回");
    }

    @Test
    void derivedStatus_noK_shouldBeNotStarted() {
        loginAsNewUser();
        objectiveService.create(objReq("2026-07", "目标"));

        ObjectiveVO vo = objectiveService.list("2026-07").get(0);
        assertEquals("0", vo.getStatus(), "无K → 未开始（非已取消）");
        assertEquals(0, vo.getCancelledCount());
    }

    @Test
    void updateKeyResult_cancelledCannotChangePlan_shouldThrow() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "目标")).getId();
        Long k1 = keyResultService.create(krCreate(objId, "K1")).getId();
        cancel(k1, "需求取消");

        KeyResultUpdateReq upd = new KeyResultUpdateReq();
        upd.setPlanCompleteDate(LocalDate.of(2026, 8, 1));
        BusinessException ex = assertThrows(BusinessException.class, () -> keyResultService.update(k1, upd));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode(), "已取消禁改计划完成时间");
    }

    @Test
    void deleteKeyResult_cancelled_ok() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "目标")).getId();
        Long k1 = keyResultService.create(krCreate(objId, "K1")).getId();
        cancel(k1, "需求取消");

        keyResultService.delete(k1); // 已取消允许删除
        assertThrows(BusinessException.class, () -> keyResultService.update(k1, krUpdate("x")));
    }

    @Test
    void deleteObjective_withCancelledK_ok() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "目标")).getId();
        Long k1 = keyResultService.create(krCreate(objId, "K1")).getId();
        cancel(k1, "需求取消");

        objectiveService.delete(objId); // 仅含已取消K -> 允许删O
        assertTrue(objectiveService.list("2026-07").isEmpty());
    }

    // ===================== 成果记录 R（v2.1）=====================

    /** 切到完成并提交 R */
    private KeyResultVO markDoneWithRecords(Long krId, List<String> records) {
        KeyResultStatusReq done = new KeyResultStatusReq();
        done.setStatus(KeyResultStatus.DONE);
        done.setRecords(records);
        return keyResultService.changeStatus(krId, done);
    }

    @Test
    void changeStatus_toDone_withRecords_shouldSaveRecords() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "目标")).getId();
        Long k1 = keyResultService.create(krCreate(objId, "K1")).getId();

        KeyResultVO done = markDoneWithRecords(k1, List.of("成果A", "成果B", "成果C"));

        assertEquals("2", done.getStatus());
        assertEquals(3, done.getRecords().size(), "VO 应返回 3 条 R");
        assertEquals(List.of("成果A", "成果B", "成果C"), done.getRecords());
        // DB 实际落库
        assertEquals(3, keyResultRecordService.listContentsByKeyResultId(k1).size(), "DB 应落 3 条 R");
    }

    @Test
    void records_shouldPreserveSubmitOrder() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "目标")).getId();
        Long k1 = keyResultService.create(krCreate(objId, "K1")).getId();

        // 按特定顺序提交
        markDoneWithRecords(k1, List.of("C3", "A1", "B2"));
        // 查询返回顺序应与提交顺序一致（后端按自增 id 升序，即落库顺序）
        assertEquals(List.of("C3", "A1", "B2"), keyResultRecordService.listContentsByKeyResultId(k1),
                "R 顺序应与提交顺序一致");

        // 返工后调整顺序再提交，顺序随之更新
        markInProgress(k1);
        markDoneWithRecords(k1, List.of("B2", "C3", "A1"));
        assertEquals(List.of("B2", "C3", "A1"), keyResultRecordService.listContentsByKeyResultId(k1),
                "调整顺序后提交，返回顺序应更新");
    }

    @Test
    void changeStatus_toDone_emptyRecords_shouldClearExisting() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "目标")).getId();
        Long k1 = keyResultService.create(krCreate(objId, "K1")).getId();
        // 先填 2 条 R
        markDoneWithRecords(k1, List.of("成果A", "成果B"));
        // 返工
        markInProgress(k1);
        // 再次切完成，传空列表 -> 清空旧 R
        KeyResultVO done = markDoneWithRecords(k1, List.of());

        assertTrue(done.getRecords().isEmpty(), "空列表应清空 R");
        assertTrue(keyResultRecordService.listContentsByKeyResultId(k1).isEmpty(), "DB 应无 R");
    }

    @Test
    void changeStatus_toDone_nullRecords_shouldClearExisting() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "目标")).getId();
        Long k1 = keyResultService.create(krCreate(objId, "K1")).getId();
        // 先填 2 条 R
        markDoneWithRecords(k1, List.of("成果A", "成果B"));
        // 返工
        markInProgress(k1);
        // 再次切完成，不传 records(null) -> 清空旧 R
        KeyResultStatusReq done = new KeyResultStatusReq();
        done.setStatus(KeyResultStatus.DONE);
        KeyResultVO vo = keyResultService.changeStatus(k1, done);

        assertTrue(vo.getRecords().isEmpty(), "null 应清空 R");
        assertTrue(keyResultRecordService.listContentsByKeyResultId(k1).isEmpty(), "DB 应无 R");
    }

    @Test
    void changeStatus_toDone_replaceRecords_shouldOverwrite() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "目标")).getId();
        Long k1 = keyResultService.create(krCreate(objId, "K1")).getId();
        // 先填 2 条 R
        markDoneWithRecords(k1, List.of("旧成果A", "旧成果B"));
        // 返工
        markInProgress(k1);
        // 再次切完成，传新内容 -> 全量覆盖
        KeyResultVO done = markDoneWithRecords(k1, List.of("新成果X"));

        assertEquals(1, done.getRecords().size(), "应覆盖为 1 条");
        assertEquals("新成果X", done.getRecords().get(0));
        assertEquals(List.of("新成果X"), keyResultRecordService.listContentsByKeyResultId(k1));
    }

    @Test
    void changeStatus_doneToInProgress_shouldKeepRecords() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "目标")).getId();
        Long k1 = keyResultService.create(krCreate(objId, "K1")).getId();
        markDoneWithRecords(k1, List.of("成果A", "成果B"));

        // 切回进行中：R 保留不动
        KeyResultVO inProgress = markInProgress(k1);

        assertEquals("1", inProgress.getStatus());
        assertEquals(2, inProgress.getRecords().size(), "切回进行中 R 应保留");
        assertEquals(2, keyResultRecordService.listContentsByKeyResultId(k1).size(), "DB 中 R 不变");
    }

    @Test
    void changeStatus_cancel_shouldKeepRecords() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "目标")).getId();
        Long k1 = keyResultService.create(krCreate(objId, "K1")).getId();
        markDoneWithRecords(k1, List.of("成果A", "成果B"));

        // 已完成 -> 进行中 -> 已取消（完成不能直接切取消，需经进行中）
        markInProgress(k1);
        KeyResultVO cancelled = cancel(k1, "需求取消");

        assertEquals("3", cancelled.getStatus());
        assertEquals(2, cancelled.getRecords().size(), "取消后 R 应保留");
        assertEquals(2, keyResultRecordService.listContentsByKeyResultId(k1).size(), "DB 中 R 保留");
    }

    @Test
    void deleteKeyResult_shouldCascadeDeleteRecords() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "目标")).getId();
        Long k1 = keyResultService.create(krCreate(objId, "K1")).getId();
        markDoneWithRecords(k1, List.of("成果A", "成果B"));

        keyResultService.delete(k1);

        assertTrue(keyResultRecordService.listContentsByKeyResultId(k1).isEmpty(), "删 K 应级联删 R");
    }

    @Test
    void deleteObjective_shouldCascadeDeleteRecords() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "目标")).getId();
        Long k1 = keyResultService.create(krCreate(objId, "K1")).getId();
        Long k2 = keyResultService.create(krCreate(objId, "K2")).getId();
        markDoneWithRecords(k1, List.of("成果A"));
        markDoneWithRecords(k2, List.of("成果B", "成果C"));
        // 含已完成 K 不允许删 O，先把两个 K 都删掉（连带删 R），再删 O
        keyResultService.delete(k1);
        keyResultService.delete(k2);

        objectiveService.delete(objId);

        assertTrue(keyResultRecordService.listContentsByKeyResultId(k1).isEmpty());
        assertTrue(keyResultRecordService.listContentsByKeyResultId(k2).isEmpty());
    }

    @Test
    void listObjectives_shouldCarryRecords() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "目标")).getId();
        Long k1 = keyResultService.create(krCreate(objId, "K1")).getId();
        Long k2 = keyResultService.create(krCreate(objId, "K2")).getId();
        markDoneWithRecords(k1, List.of("K1成果A", "K1成果B"));
        // k2 不完成，无 R

        List<ObjectiveVO> list = objectiveService.list("2026-07");
        assertEquals(1, list.size());
        KeyResultVO kr1Vo = list.get(0).getKeyResults().stream()
                .filter(k -> k.getId().equals(k1)).findFirst().orElseThrow();
        KeyResultVO kr2Vo = list.get(0).getKeyResults().stream()
                .filter(k -> k.getId().equals(k2)).findFirst().orElseThrow();
        assertEquals(List.of("K1成果A", "K1成果B"), kr1Vo.getRecords(), "K1 应携带 2 条 R");
        assertTrue(kr2Vo.getRecords().isEmpty(), "K2 无 R 应为空列表");
    }

    @Test
    void records_shouldIsolateByUser() {
        Long userA = loginAsNewUser();
        Long objA = objectiveService.create(objReq("2026-07", "A目标")).getId();
        Long kA = keyResultService.create(krCreate(objA, "A的K")).getId();
        markDoneWithRecords(kA, List.of("A的成果"));

        // 切换用户 B
        loginAsNewUser();
        Long objB = objectiveService.create(objReq("2026-07", "B目标")).getId();
        Long kB = keyResultService.create(krCreate(objB, "B的K")).getId();
        markDoneWithRecords(kB, List.of("B的成果1", "B的成果2"));

        // B 查自己的绩效列表，不应含 A 的 R
        List<ObjectiveVO> listB = objectiveService.list("2026-07");
        assertEquals(1, listB.size(), "B 只应看到自己的目标");
        assertEquals("B目标", listB.get(0).getName());
        KeyResultVO krB = listB.get(0).getKeyResults().get(0);
        assertEquals(List.of("B的成果1", "B的成果2"), krB.getRecords());
    }

    @Test
    void createKeyResult_cancelledAtCreate_shouldThrow() {
        loginAsNewUser();
        Long objId = objectiveService.create(objReq("2026-07", "目标")).getId();
        KeyResultCreateReq req = krCreate(objId, "K1");
        req.setStatus(KeyResultStatus.CANCELLED);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> keyResultService.create(req));
        assertEquals(ResultCode.PARAM_VALIDATE_FAILED.getCode(), ex.getCode(), "创建不允许已取消状态");
    }

    private KeyResultVO cancel(Long krId, String reason) {
        KeyResultStatusReq cancel = new KeyResultStatusReq();
        cancel.setStatus(KeyResultStatus.CANCELLED);
        cancel.setCancelReason(reason);
        return keyResultService.changeStatus(krId, cancel);
    }

    private KeyResultVO markDone(Long krId) {
        KeyResultStatusReq done = new KeyResultStatusReq();
        done.setStatus(KeyResultStatus.DONE);
        return keyResultService.changeStatus(krId, done);
    }

    private KeyResultVO markInProgress(Long krId) {
        KeyResultStatusReq inProgress = new KeyResultStatusReq();
        inProgress.setStatus(KeyResultStatus.IN_PROGRESS);
        return keyResultService.changeStatus(krId, inProgress);
    }

    private ObjectiveCreateReq objReq(String month, String name) {
        ObjectiveCreateReq r = new ObjectiveCreateReq();
        r.setMonth(month);
        r.setName(name);
        return r;
    }

    private ObjectiveUpdateReq updReq(String name) {
        ObjectiveUpdateReq r = new ObjectiveUpdateReq();
        r.setName(name);
        return r;
    }

    private KeyResultCreateReq krCreate(Long objId, String name) {
        KeyResultCreateReq r = new KeyResultCreateReq();
        r.setObjectiveId(objId);
        r.setName(name);
        return r;
    }

    private KeyResultCreateReq krCreateWithPlan(Long objId, String name, String date) {
        KeyResultCreateReq r = krCreate(objId, name);
        r.setPlanCompleteDate(LocalDate.parse(date));
        return r;
    }

    private KeyResultUpdateReq krUpdate(String name) {
        KeyResultUpdateReq r = new KeyResultUpdateReq();
        r.setName(name);
        return r;
    }
}
