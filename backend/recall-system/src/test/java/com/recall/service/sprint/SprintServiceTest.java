package com.recall.service.sprint;

import com.recall.BaseTest;
import com.recall.common.api.ResultCode;
import com.recall.common.exception.BusinessException;
import com.recall.dto.objectives.KeyResultStatusReq;
import com.recall.dto.sprint.SprintCreateReq;
import com.recall.dto.sprint.SprintInvolvedReq;
import com.recall.dto.sprint.SprintLinkReq;
import com.recall.dto.sprint.SprintStatusReq;
import com.recall.dto.sprint.SprintUpdateReq;
import com.recall.dto.objectives.ObjectiveCreateReq;
import com.recall.dto.objectives.KeyResultCreateReq;
import com.recall.enums.KeyResultStatus;
import com.recall.enums.SprintStatus;
import com.recall.service.objectives.KeyResultService;
import com.recall.service.objectives.ObjectiveService;
import com.recall.vo.objectives.KeyResultVO;
import com.recall.vo.objectives.ObjectiveVO;
import com.recall.vo.sprint.SprintItemVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 团队冲刺业务逻辑测试。
 * <p>
 * 重点：关联 K、K 状态联动同步、需我介入切换、数据隔离。
 *
 * @author recall
 */
@Transactional
class SprintServiceTest extends BaseTest {

    @Autowired
    private SprintService sprintService;
    @Autowired
    private ObjectiveService objectiveService;
    @Autowired
    private KeyResultService keyResultService;

    @Test
    void create_basic_statusNotStarted() {
        loginAsNewUser();
        SprintItemVO vo = sprintService.create(newItemReq("2026-07", "任务A"));
        assertEquals("0", vo.getStatus(), "新建默认未开始");
        assertFalse(vo.getNeedInvolved());
        assertTrue(vo.getKeyResultIds().isEmpty());
    }

    @Test
    void create_duplicateTitleSameMonth_shouldThrow() {
        loginAsNewUser();
        sprintService.create(newItemReq("2026-07", "任务A"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sprintService.create(newItemReq("2026-07", "任务A")));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void create_duplicateTitleDifferentMonth_ok() {
        loginAsNewUser();
        sprintService.create(newItemReq("2026-07", "任务A"));

        // 跨月重名允许
        SprintItemVO vo = sprintService.create(newItemReq("2026-08", "任务A"));
        assertEquals("2026-08", vo.getMonth());
    }

    @Test
    void update_renameToExisting_shouldThrow() {
        loginAsNewUser();
        sprintService.create(newItemReq("2026-07", "任务A"));
        Long bId = sprintService.create(newItemReq("2026-07", "任务B")).getId();

        SprintUpdateReq upd = new SprintUpdateReq();
        upd.setTitle("任务A");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sprintService.update(bId, upd));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }
    @Test
    void list_filterNeedInvolved() {
        loginAsNewUser();
        createInvolved("2026-07", "需介入");
        sprintService.create(newItemReq("2026-07", "普通任务"));

        assertEquals(2, sprintService.list("2026-07", null).size());
        assertEquals(1, sprintService.list("2026-07", true).size());
    }

    @Test
    void linkKeyResults_notInvolved_shouldThrow() {
        loginAsNewUser();
        Long sprintId = sprintService.create(newItemReq("2026-07", "未介入任务")).getId();

        SprintLinkReq link = new SprintLinkReq();
        link.setKeyResultIds(List.of(1L));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sprintService.linkKeyResults(sprintId, link));
        assertEquals(ResultCode.PARAM_VALIDATE_FAILED.getCode(), ex.getCode());
    }

    @Test
    void linkKeyResults_allNotStarted_statusNotStarted() {
        loginAsNewUser();
        Long krId = createKr("2026-07", "目标", "K1");
        Long krId2 = createKr("2026-07", "目标2", "K2");
        Long sprintId = createInvolved("2026-07", "介入任务").getId();

        SprintLinkReq link = new SprintLinkReq();
        link.setKeyResultIds(List.of(krId, krId2));
        SprintItemVO vo = sprintService.linkKeyResults(sprintId, link);

        assertEquals(2, vo.getKeyResultIds().size());
        // K 全未开始 → 冲刺未开始（新规则）
        assertEquals("0", vo.getStatus(), "关联的K全未开始 → 冲刺未开始");
    }

    @Test
    void linkKeyResults_allDone_statusDone() {
        loginAsNewUser();
        Long krId = createKr("2026-07", "目标", "K1");
        Long krId2 = createKr("2026-07", "目标2", "K2");
        markKrDone(krId);
        markKrDone(krId2);
        Long sprintId = createInvolved("2026-07", "介入任务").getId();

        SprintLinkReq link = new SprintLinkReq();
        link.setKeyResultIds(List.of(krId, krId2));
        SprintItemVO vo = sprintService.linkKeyResults(sprintId, link);
        assertEquals("2", vo.getStatus(), "关联的K全已完成 → 冲刺已完成");
    }

    @Test
    void linkKeyResults_mixedStatus_statusInProgress() {
        loginAsNewUser();
        Long krId = createKr("2026-07", "目标", "K1");
        Long krId2 = createKr("2026-07", "目标2", "K2");
        markKrDone(krId2);
        Long sprintId = createInvolved("2026-07", "介入任务").getId();

        SprintLinkReq link = new SprintLinkReq();
        link.setKeyResultIds(List.of(krId, krId2));
        SprintItemVO vo = sprintService.linkKeyResults(sprintId, link);
        assertEquals("1", vo.getStatus(), "未开始与已完成混合 → 冲刺进行中");
    }

    @Test
    void linkKeyResults_cancelledExcluded_statusByRemaining() {
        loginAsNewUser();
        Long krDone = createKr("2026-07", "目标", "Kdone");
        markKrDone(krDone);
        Long krCancelled = createKr("2026-07", "目标2", "Kcancel");
        cancelKr(krCancelled);
        Long sprintId = createInvolved("2026-07", "介入任务").getId();

        SprintLinkReq link = new SprintLinkReq();
        link.setKeyResultIds(List.of(krDone, krCancelled));
        SprintItemVO vo = sprintService.linkKeyResults(sprintId, link);
        // 排除已取消 K 后，剩余唯一有效 K 已完成 → 冲刺已完成
        assertEquals("2", vo.getStatus(), "已取消K不参与统计，剩余全完成 → 冲刺已完成");
    }

    @Test
    void linkKeyResults_cancelAll_statusBackToNotStarted() {
        loginAsNewUser();
        Long krId = createKr("2026-07", "目标", "K1");
        Long sprintId = createInvolved("2026-07", "介入任务").getId();

        SprintLinkReq link = new SprintLinkReq();
        link.setKeyResultIds(List.of(krId));
        sprintService.linkKeyResults(sprintId, link);

        // 取消全部关联
        SprintLinkReq cancel = new SprintLinkReq();
        cancel.setKeyResultIds(List.of());
        SprintItemVO vo = sprintService.linkKeyResults(sprintId, cancel);
        assertEquals("0", vo.getStatus(), "取消全部关联 → 状态回未开始");
        assertTrue(vo.getKeyResultIds().isEmpty());
    }

    @Test
    void linkKeyResults_nullIds_treatedAsClear() {
        loginAsNewUser();
        Long krId = createKr("2026-07", "目标", "K1");
        Long sprintId = createInvolved("2026-07", "介入任务").getId();

        SprintLinkReq link = new SprintLinkReq();
        link.setKeyResultIds(List.of(krId));
        sprintService.linkKeyResults(sprintId, link);

        // keyResultIds 为 null（Service 直调场景）视为清除全部关联
        SprintLinkReq nullReq = new SprintLinkReq();
        nullReq.setKeyResultIds(null);
        SprintItemVO vo = sprintService.linkKeyResults(sprintId, nullReq);
        assertEquals("0", vo.getStatus(), "null 视为清除关联 → 状态回未开始");
        assertTrue(vo.getKeyResultIds().isEmpty());
    }

    @Test
    void toggleInvolved_false_clearsLinks() {
        loginAsNewUser();
        Long krId = createKr("2026-07", "目标", "K1");
        Long sprintId = createInvolved("2026-07", "介入任务").getId();

        SprintLinkReq link = new SprintLinkReq();
        link.setKeyResultIds(List.of(krId));
        sprintService.linkKeyResults(sprintId, link);

        // 切换为无需介入 → 清空关联
        SprintInvolvedReq off = new SprintInvolvedReq();
        off.setNeedInvolved(false);
        SprintItemVO vo = sprintService.toggleInvolved(sprintId, off);
        assertFalse(vo.getNeedInvolved());
        assertTrue(vo.getKeyResultIds().isEmpty(), "切为无需介入 → 清空关联");
    }

    @Test
    void kChangeStatus_sprintSynced() {
        loginAsNewUser();
        Long krId = createKr("2026-07", "目标", "K1");
        Long sprintId = createInvolved("2026-07", "介入任务").getId();

        SprintLinkReq link = new SprintLinkReq();
        link.setKeyResultIds(List.of(krId));
        sprintService.linkKeyResults(sprintId, link);

        // K → 已完成 → 冲刺应同步为已完成
        markKrDone(krId);
        SprintItemVO vo = sprintService.list("2026-07", null).get(0);
        assertEquals("2", vo.getStatus(), "K完成后冲刺应同步为已完成");
    }

    @Test
    void kChangeStatus_toInProgress_sprintSynced() {
        loginAsNewUser();
        Long krId = createKr("2026-07", "目标", "K1");
        Long sprintId = createInvolved("2026-07", "介入任务").getId();

        SprintLinkReq link = new SprintLinkReq();
        link.setKeyResultIds(List.of(krId));
        sprintService.linkKeyResults(sprintId, link);

        // K → 进行中 → 冲刺同步为进行中
        KeyResultStatusReq inProgress = new KeyResultStatusReq();
        inProgress.setStatus(KeyResultStatus.IN_PROGRESS);
        keyResultService.changeStatus(krId, inProgress);

        SprintItemVO vo = sprintService.list("2026-07", null).get(0);
        assertEquals("1", vo.getStatus(), "K进行中 → 冲刺进行中");
    }

    @Test
    void kCancelled_rederiveByRemainingK() {
        loginAsNewUser();
        Long krNotStarted = createKr("2026-07", "目标", "Ktodo");
        Long krInProgress = createKr("2026-07", "目标2", "Kdoing");
        markKrInProgress(krInProgress);
        Long sprintId = createInvolved("2026-07", "介入任务").getId();

        // 关联 Ktodo(未开始) + Kdoing(进行中) → 含进行中 → 进行中
        SprintLinkReq link = new SprintLinkReq();
        link.setKeyResultIds(List.of(krNotStarted, krInProgress));
        SprintItemVO vo = sprintService.linkKeyResults(sprintId, link);
        assertEquals("1", vo.getStatus(), "未开始+进行中 → 进行中");

        // Kdoing → 已取消：重新派生时排除已取消 K，剩余 Ktodo 未开始 → 冲刺未开始
        cancelKr(krInProgress);
        SprintItemVO after = sprintService.list("2026-07", null).get(0);
        assertEquals("0", after.getStatus(), "K取消后排除该K，剩余全未开始 → 冲刺未开始");
    }

    @Test
    void changeStatus_involved_legalTransition() {
        loginAsNewUser();
        Long sprintId = createInvolved("2026-07", "介入任务").getId();

        // 未开始 → 进行中（合法）
        SprintStatusReq inProgress = new SprintStatusReq();
        inProgress.setStatus(SprintStatus.IN_PROGRESS);
        SprintItemVO vo = sprintService.changeStatus(sprintId, inProgress);
        assertEquals("1", vo.getStatus());

        // 进行中 → 已完成（合法）
        SprintStatusReq done = new SprintStatusReq();
        done.setStatus(SprintStatus.DONE);
        vo = sprintService.changeStatus(sprintId, done);
        assertEquals("2", vo.getStatus());

        // 已完成 → 进行中（返工，合法）
        vo = sprintService.changeStatus(sprintId, inProgress);
        assertEquals("1", vo.getStatus());
    }

    @Test
    void changeStatus_illegalTransition_shouldThrow() {
        loginAsNewUser();
        Long sprintId = createInvolved("2026-07", "介入任务").getId();

        // 未开始 → 已完成（跳级完成）非法
        SprintStatusReq done = new SprintStatusReq();
        done.setStatus(SprintStatus.DONE);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sprintService.changeStatus(sprintId, done));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode(), "未开始→已完成非法");

        // 未开始 → 未开始（同状态）非法
        SprintStatusReq same = new SprintStatusReq();
        same.setStatus(SprintStatus.NOT_STARTED);
        ex = assertThrows(BusinessException.class,
                () -> sprintService.changeStatus(sprintId, same));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode(), "同状态流转非法");
    }

    @Test
    void changeStatus_notInvolved_shouldThrow() {
        loginAsNewUser();
        Long sprintId = sprintService.create(newItemReq("2026-07", "普通任务")).getId();

        SprintStatusReq done = new SprintStatusReq();
        done.setStatus(SprintStatus.DONE);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sprintService.changeStatus(sprintId, done));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode(), "无需介入的冲刺任务不可更改状态");
    }

    @Test
    void changeStatus_linkedKr_shouldThrow() {
        loginAsNewUser();
        Long krId = createKr("2026-07", "目标", "K1");
        Long sprintId = createInvolved("2026-07", "介入任务").getId();

        // 关联 K 后，状态由 K 联动，禁止手动变更
        SprintLinkReq link = new SprintLinkReq();
        link.setKeyResultIds(List.of(krId));
        sprintService.linkKeyResults(sprintId, link);

        SprintStatusReq done = new SprintStatusReq();
        done.setStatus(SprintStatus.DONE);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sprintService.changeStatus(sprintId, done));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode(), "已关联K的冲刺任务不可手动变更状态");
    }

    @Test
    void deleteKr_recomputeLinkedSprint() {
        loginAsNewUser();
        Long krInProgress = createKr("2026-07", "目标", "Kdoing");
        markKrInProgress(krInProgress);
        Long krNotStarted = createKr("2026-07", "目标2", "Ktodo");
        Long sprintId = createInvolved("2026-07", "介入任务").getId();

        // 关联 Kdoing(进行中) + Ktodo(未开始) → 含进行中 → 进行中
        SprintLinkReq link = new SprintLinkReq();
        link.setKeyResultIds(List.of(krInProgress, krNotStarted));
        SprintItemVO vo = sprintService.linkKeyResults(sprintId, link);
        assertEquals("1", vo.getStatus(), "进行中+未开始 → 进行中");

        // 删 Kdoing → 剩 Ktodo 未开始 → 冲刺重算为未开始，且关联清理
        keyResultService.delete(krInProgress);
        SprintItemVO after = sprintService.list("2026-07", null).get(0);
        assertEquals("0", after.getStatus(), "删K后按剩余K重算 → 未开始");
        assertEquals(1, after.getKeyResultIds().size(), "删K后关联同步清理");
    }

    @Test
    void deleteObjective_recomputeLinkedSprint() {
        loginAsNewUser();
        // O 下一个 K，被冲刺关联，且 K 进行中 → 冲刺进行中
        Long krInProgress = createKr("2026-07", "目标", "Kdoing");
        markKrInProgress(krInProgress);
        Long sprintId = createInvolved("2026-07", "介入任务").getId();

        SprintLinkReq link = new SprintLinkReq();
        link.setKeyResultIds(List.of(krInProgress));
        SprintItemVO vo = sprintService.linkKeyResults(sprintId, link);
        assertEquals("1", vo.getStatus(), "K进行中 → 冲刺进行中");

        // 删 O 级联删 K → 冲刺关联被清理、状态重算为未开始
        // 通过 K 反查 O id（createKr 内部建了 O）
        Long objId = objectiveService.list("2026-07").get(0).getId();
        objectiveService.delete(objId);
        SprintItemVO after = sprintService.list("2026-07", null).get(0);
        assertEquals("0", after.getStatus(), "删O级联删K → 冲刺无关联 → 未开始");
        assertTrue(after.getKeyResultIds().isEmpty(), "删O后冲刺关联被清理");
    }

    @Test
    void linkKeyResults_duplicateIds_deduped() {
        loginAsNewUser();
        Long krId = createKr("2026-07", "目标", "K1");
        Long sprintId = createInvolved("2026-07", "介入任务").getId();

        // 传入重复 ID，应去重后只建一条关联
        SprintLinkReq link = new SprintLinkReq();
        link.setKeyResultIds(List.of(krId, krId, krId));
        SprintItemVO vo = sprintService.linkKeyResults(sprintId, link);
        assertEquals(1, vo.getKeyResultIds().size(), "重复ID去重后只1条关联");
    }

    @Test
    void delete_shouldRemoveLinks() {
        loginAsNewUser();
        Long krId = createKr("2026-07", "目标", "K1");
        Long sprintId = createInvolved("2026-07", "介入任务").getId();

        SprintLinkReq link = new SprintLinkReq();
        link.setKeyResultIds(List.of(krId));
        sprintService.linkKeyResults(sprintId, link);

        sprintService.delete(sprintId);
        assertTrue(sprintService.list("2026-07", null).isEmpty());
    }

    @Test
    void dataIsolation_userCannotAccessOthers() {
        loginAsNewUser();
        Long sprintId = sprintService.create(newItemReq("2026-07", "私有任务")).getId();

        loginAsNewUser();
        SprintUpdateReq hack = new SprintUpdateReq();
        hack.setTitle("篡改");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sprintService.update(sprintId, hack));
        assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
    }

    // ===================== 辅助 =====================

    private void markKrDone(Long krId) {
        KeyResultStatusReq done = new KeyResultStatusReq();
        done.setStatus(KeyResultStatus.DONE);
        keyResultService.changeStatus(krId, done);
    }

    /** 将 K 从未开始切换为进行中 */
    private void markKrInProgress(Long krId) {
        KeyResultStatusReq inProgress = new KeyResultStatusReq();
        inProgress.setStatus(KeyResultStatus.IN_PROGRESS);
        keyResultService.changeStatus(krId, inProgress);
    }

    /** 将 K 切换为已取消（需带取消原因） */
    private void cancelKr(Long krId) {
        KeyResultStatusReq cancel = new KeyResultStatusReq();
        cancel.setStatus(KeyResultStatus.CANCELLED);
        cancel.setCancelReason("不需要了");
        keyResultService.changeStatus(krId, cancel);
    }

    /** 创建目标 + 关键成果，返回 K 的 id */
    private Long createKr(String month, String objName, String krName) {
        ObjectiveCreateReq objReq = new ObjectiveCreateReq();
        objReq.setMonth(month);
        objReq.setName(objName);
        Long objId = objectiveService.create(objReq).getId();

        KeyResultCreateReq krReq = new KeyResultCreateReq();
        krReq.setObjectiveId(objId);
        krReq.setName(krName);
        return keyResultService.create(krReq).getId();
    }

    private SprintCreateReq newItemReq(String month, String title) {
        SprintCreateReq r = new SprintCreateReq();
        r.setMonth(month);
        r.setTitle(title);
        return r;
    }

    /** 创建冲刺任务并切换为需我介入（创建本身不填 needInvolved，通过专门接口切换） */
    private SprintItemVO createInvolved(String month, String title) {
        SprintItemVO vo = sprintService.create(newItemReq(month, title));
        SprintInvolvedReq on = new SprintInvolvedReq();
        on.setNeedInvolved(true);
        return sprintService.toggleInvolved(vo.getId(), on);
    }
}
