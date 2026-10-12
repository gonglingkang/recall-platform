package com.recall.service.requirement.impl;

import com.recall.common.api.ResultCode;
import com.recall.common.exception.BusinessException;
import com.recall.service.llm.LlmService;
import com.recall.service.requirement.MeetingMinutesTitleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 会议纪要生成需求标题 Service 实现。
 * <p>
 * 只做措辞转写不新增语义：去掉日期前缀与「会议纪要/传达/传递」等会议性描述，
 * 保留核心需求点；输出校验非空与长度。
 *
 * @author recall
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingMinutesTitleServiceImpl implements MeetingMinutesTitleService {

    private static final String SYSTEM_PROMPT = """
            你是需求标题生成助手。用户会给出一条会议纪要的标题，请把它转写为简洁规范的需求标题：
            1. 去掉日期数字前缀（如 20261010）与「会议纪要」「传达」「传递」「补充传递会议」等会议性描述；
            2. 保留核心需求点与业务对象（如「统一标签(增/副/合并)」），不新增原文没有的信息；
            3. 不超过 30 个字，通顺、像一条需求名称；
            4. 只输出标题本身，不要任何解释、引号或标点后缀。""";

    private final LlmService llmService;

    @Override
    public String generateRequirementTitle(String minutesTitle) {
        if (minutesTitle == null || minutesTitle.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_VALIDATE_FAILED, "纪要标题不能为空");
        }
        String output = llmService.chat(SYSTEM_PROMPT, "会议纪要标题：" + minutesTitle.trim());
        if (output == null || output.isBlank()) {
            throw new BusinessException(ResultCode.LLM_RESPONSE_INVALID, "标题生成结果为空，请重试");
        }
        String title = output.trim().replaceAll("^[\"“]|[\"”]$", "");
        if (title.length() > 200) {
            title = title.substring(0, 200);
        }
        return title;
    }
}
