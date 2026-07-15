package com.recall.service;

import com.recall.BaseTest;
import com.recall.common.exception.BusinessException;
import com.recall.dto.objectives.KeyResultCreateReq;
import com.recall.dto.objectives.KeyResultStatusReq;
import com.recall.dto.objectives.ObjectiveCreateReq;
import com.recall.dto.sprint.SprintCreateReq;
import com.recall.dto.sprint.SprintInvolvedReq;
import com.recall.dto.sprint.SprintStatusReq;
import com.recall.enums.KeyResultStatus;
import com.recall.enums.SprintStatus;
import com.recall.service.objectives.KeyResultService;
import com.recall.service.objectives.ObjectiveService;
import com.recall.service.plan.PlanService;
import com.recall.service.sprint.SprintService;
import com.recall.vo.plan.MonthTrendVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 月度趋势业务逻辑测试。
 *
 * @author recall
 */
@Transactional
class PlanAndStatsTest extends BaseTest {

    @Autowired
    private PlanService planService;
    @Autowired
    private ObjectiveService objectiveService;
    @Autowired
    private KeyResultService keyResultService;
    @Autowired
    private SprintService sprintService;

    @Test
    void trend_shouldReturnMonthlyRates() {
        loginAsNewUser();
        // 目标 O 下 2 个 K，完成 1 个 → 绩效率 0.5
        Long objId = objectiveService.create(objReq("2026-06", "目标1")).getId();
        Long kr1 = keyResultService.create(krReq(objId, "KR1")).getId();
        keyResultService.create(krReq(objId, "KR2"));
        markKrDone(kr1);

        // 冲刺：1 条，完成 1 条（按状态机 未开始→进行中→已完成）→ 冲刺率 1.0
        Long spId = createSprint("2026-06", "冲刺1");
        markSprintDone(spId);

        List<MonthTrendVO> trend = planService.trend("2026-01", "2026-06");
        assertEquals(6, trend.size(), "应返回 6 个月");
        assertEquals("2026-01", trend.get(0).getMonth());
        // 1-5 月无数据，全 0
        for (int i = 0; i < 5; i++) {
            assertEquals(0.0, trend.get(i).getPerfRate(), "无数据月份绩效率=0");
            assertEquals(0.0, trend.get(i).getSprintRate(), "无数据月份冲刺率=0");
        }
        // 6 月有数据
        MonthTrendVO jun = trend.get(5);
        assertEquals("2026-06", jun.getMonth());
        assertEquals(0.5, jun.getPerfRate(), "6月绩效率=0.5");
        assertEquals(1.0, jun.getSprintRate(), "6月冲刺率=1.0");
    }

    @Test
    void trend_emptyRange_allZeros() {
        loginAsNewUser();
        List<MonthTrendVO> trend = planService.trend("2026-01", "2026-03");
        assertEquals(3, trend.size());
        assertTrue(trend.stream().allMatch(t -> t.getPerfRate() == 0.0 && t.getSprintRate() == 0.0),
                "无数据月份全 0");
    }

    @Test
    void trend_invalidRange_shouldThrow() {
        loginAsNewUser();
        // 起始晚于截止
        assertThrows(BusinessException.class, () -> planService.trend("2026-06", "2026-01"));
        // 非法格式
        assertThrows(BusinessException.class, () -> planService.trend("2026/01", "2026-06"));
        // 跨度过大（13 个月）
        assertThrows(BusinessException.class, () -> planService.trend("2025-01", "2026-02"));
    }

    // ===================== 辅助 =====================

    private void markKrDone(Long krId) {
        KeyResultStatusReq done = new KeyResultStatusReq();
        done.setStatus(KeyResultStatus.DONE);
        keyResultService.changeStatus(krId, done);
    }

    private Long createSprint(String month, String title) {
        SprintCreateReq sp = new SprintCreateReq();
        sp.setMonth(month);
        sp.setTitle(title);
        Long spId = sprintService.create(sp).getId();
        SprintInvolvedReq involved = new SprintInvolvedReq();
        involved.setNeedInvolved(true);
        sprintService.toggleInvolved(spId, involved);
        return spId;
    }

    private void markSprintDone(Long spId) {
        SprintStatusReq inProgress = new SprintStatusReq();
        inProgress.setStatus(SprintStatus.IN_PROGRESS);
        sprintService.changeStatus(spId, inProgress);
        SprintStatusReq done = new SprintStatusReq();
        done.setStatus(SprintStatus.DONE);
        sprintService.changeStatus(spId, done);
    }

    private ObjectiveCreateReq objReq(String month, String name) {
        ObjectiveCreateReq r = new ObjectiveCreateReq();
        r.setMonth(month);
        r.setName(name);
        return r;
    }

    private KeyResultCreateReq krReq(Long objId, String name) {
        KeyResultCreateReq r = new KeyResultCreateReq();
        r.setObjectiveId(objId);
        r.setName(name);
        return r;
    }
}
