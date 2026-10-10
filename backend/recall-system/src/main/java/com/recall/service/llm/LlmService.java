package com.recall.service.llm;

/**
 * 大模型对话 Service（Anthropic Messages 协议）。
 * <p>
 * 统一封装网关调用（POST {base-url}/v1/messages），调用方只关心
 * system + user 两段提示词与纯文本结果；失败抛业务异常（49xx）。
 *
 * @author recall
 */
public interface LlmService {

    /**
     * 发起一次对话补全。
     *
     * @param systemPrompt 系统提示词（角色与输出格式约束）
     * @param userPrompt   用户提示词（素材与具体任务）
     * @return 模型输出的纯文本（content 中 text 块拼接）
     */
    String chat(String systemPrompt, String userPrompt);
}
