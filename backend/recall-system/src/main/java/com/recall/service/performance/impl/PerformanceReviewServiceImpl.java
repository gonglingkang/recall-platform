package com.recall.service.performance.impl;

import com.recall.common.api.ResultCode;
import com.recall.common.exception.BusinessException;
import com.recall.service.daily.DailyReportService;
import com.recall.service.llm.LlmService;
import com.recall.service.objectives.ObjectiveService;
import com.recall.service.performance.PerformanceReviewService;
import com.recall.vo.daily.DailyReportItemVO;
import com.recall.vo.daily.DailyReportMonthVO;
import com.recall.vo.daily.DailyReportVO;
import com.recall.vo.objectives.KeyResultVO;
import com.recall.vo.objectives.ObjectiveVO;
import com.recall.vo.performance.PerformanceReviewVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.List;

/**
 * 绩效面谈内容生成 Service 实现。
 * <p>
 * 素材通过各 owner service 读取（目标 ObjectiveService / 日报 DailyReportService），
 * 本 Service 不持有任何 Mapper。模型输出按固定标记分段，缺任一段视为返回异常。
 *
 * @author recall
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceReviewServiceImpl implements PerformanceReviewService {

    private static final String SEC_HIGHLIGHTS = "【本期工作亮点】";
    private static final String SEC_SHORTCOMINGS = "【本期工作不足】";
    private static final String SEC_DEVIATION = "【偏差原因分析】";
    private static final String SEC_NEXT_FOCUS = "【下期工作重点或改进建议】";

    private static final String SYSTEM_PROMPT = """
            你是绩效面谈内容撰写助手。根据用户提供的月度素材，生成绩效面谈四部分内容。
            要求：
            1. 语言简洁专业，第一人称视角；
            2. 每部分1~3条，每条一句话，四部分合计不超过250字；
            3. 站在整月总结的角度归纳：把同类日常工作合并概括成模块/项目层面的成果，
               禁止罗列单日具体事项，输出中不得出现具体日期；
            4. 只依据素材归纳，不虚构。判定完成与否以关键成果的状态为准：
               状态为已完成的关键成果不得写成不足或未收尾；日报中的进度百分比只是
               当天快照，不得据此判定该项工作整月未完成；不足与偏差只从「进行中/
               未开始/已取消」的关键成果或延期（实际完成晚于计划完成）的项中提炼；
               若本期确实全部按期完成，不足部分可从流程、效率、协作角度提炼轻量改进点；
            5. 下期工作重点优先依据素材中【下月规划目标与关键成果】提炼；
               没有下月规划素材时，再基于本期未完成事项与改进方向提炼；
            6. 输出必须严格使用以下标记分段，标记之外不输出任何内容：
            【本期工作亮点】
            …
            【本期工作不足】
            …
            【偏差原因分析】
            …
            【下期工作重点或改进建议】
            …""";

    private final ObjectiveService objectiveService;
    private final DailyReportService dailyReportService;
    private final LlmService llmService;

    @Override
    public PerformanceReviewVO generate(String month) {
        String nextMonth = YearMonth.parse(month).plusMonths(1).toString();
        String material = buildMaterial(month, nextMonth);
        String userPrompt = "以下是 " + month + " 的月度工作素材，请生成绩效面谈内容。"
                + "下期指 " + nextMonth + "。\n\n" + material;
        String output = llmService.chat(SYSTEM_PROMPT, userPrompt);
        PerformanceReviewVO vo = PerformanceReviewVO.builder()
                .highlights(section(output, SEC_HIGHLIGHTS, SEC_SHORTCOMINGS))
                .shortcomings(section(output, SEC_SHORTCOMINGS, SEC_DEVIATION))
                .deviationAnalysis(section(output, SEC_DEVIATION, SEC_NEXT_FOCUS))
                .nextFocus(section(output, SEC_NEXT_FOCUS, null))
                .build();
        if (isBlank(vo.getHighlights()) || isBlank(vo.getShortcomings())
                || isBlank(vo.getDeviationAnalysis()) || isBlank(vo.getNextFocus())) {
            log.warn("绩效面谈输出缺少分段: month={}, output={}", month, abbreviate(output));
            throw new BusinessException(ResultCode.LLM_RESPONSE_INVALID);
        }
        return vo;
    }

    /** 汇总素材：本月目标/日报为必要素材，下月规划为下期重点的依据；本月目标与日报均为空抛 4903 */
    private String buildMaterial(String month, String nextMonth) {
        StringBuilder sb = new StringBuilder();
        boolean hasObjective = false;
        List<ObjectiveVO> objectives = objectiveService.list(month);
        if (!objectives.isEmpty()) {
            sb.append("【绩效目标与关键成果】\n");
            for (ObjectiveVO objective : objectives) {
                sb.append("目标：").append(objective.getName()).append('\n');
                for (KeyResultVO kr : objective.getKeyResults()) {
                    hasObjective = true;
                    appendKrLine(sb, kr);
                }
            }
        }
        boolean hasDaily = false;
        DailyReportMonthVO monthVo = dailyReportService.monthList(month);
        if (monthVo.getReports() != null) {
            StringBuilder dailySb = new StringBuilder();
            for (DailyReportVO report : monthVo.getReports()) {
                if (report.getItems() == null || report.getItems().isEmpty()) {
                    continue;
                }
                hasDaily = true;
                for (DailyReportItemVO item : report.getItems()) {
                    // 不带条目进度：日报进度是当天快照，会误导模型把已完成的 KR 判成未收尾
                    dailySb.append("- ").append(item.getContent()).append('\n');
                }
            }
            if (hasDaily) {
                sb.append("【日报工作内容】\n").append(dailySb);
            }
        }
        if (!hasObjective && !hasDaily) {
            throw new BusinessException(ResultCode.LLM_MATERIAL_EMPTY);
        }
        List<ObjectiveVO> nextObjectives = objectiveService.list(nextMonth);
        if (!nextObjectives.isEmpty()) {
            sb.append("【下月规划目标与关键成果】\n");
            for (ObjectiveVO objective : nextObjectives) {
                sb.append("目标：").append(objective.getName()).append('\n');
                for (KeyResultVO kr : objective.getKeyResults()) {
                    appendKrLine(sb, kr);
                }
            }
        }
        return sb.toString();
    }

    private void appendKrLine(StringBuilder sb, KeyResultVO kr) {
        sb.append("  - ").append(kr.getName())
                .append("（状态：").append(statusCn(kr.getStatus()));
        if (kr.getPlanCompleteDate() != null) {
            sb.append("，计划完成：").append(kr.getPlanCompleteDate());
        }
        if (kr.getCompleteDate() != null) {
            sb.append("，实际完成：").append(kr.getCompleteDate());
        }
        if (kr.getRecords() != null && !kr.getRecords().isEmpty()) {
            sb.append("；成果记录：").append(String.join("；", kr.getRecords()));
        }
        sb.append("）\n");
    }

    /** 截取 start 标记到 end 标记之间的内容；end 为 null 时取到结尾，缺 start 返回 null */
    private String section(String text, String start, String end) {
        int s = text.indexOf(start);
        if (s < 0) {
            return null;
        }
        s += start.length();
        int e = end != null ? text.indexOf(end, s) : text.length();
        String body = e < 0 ? text.substring(s) : text.substring(s, e);
        return body.trim();
    }

    private String statusCn(String status) {
        return switch (status == null ? "" : status) {
            case "not_started" -> "未开始";
            case "in_progress" -> "进行中";
            case "done" -> "已完成";
            case "cancelled" -> "已取消";
            default -> status == null ? "未知" : status;
        };
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private String abbreviate(String s) {
        return s.length() <= 200 ? s : s.substring(0, 200) + "…";
    }
}
