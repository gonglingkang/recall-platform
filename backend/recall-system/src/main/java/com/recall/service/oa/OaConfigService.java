package com.recall.service.oa;

import com.recall.dto.oa.OaConfigSaveReq;
import com.recall.entity.oa.OaUserConfig;
import com.recall.vo.oa.OaConfigVO;

/**
 * OA 对接配置 Service。
 * <p>
 * 每人一份（同 user_id 唯一），upsert 保存。
 * 密码只存密文；保存时留空表示不修改；查询不回明文。
 *
 * @author recall
 */
public interface OaConfigService {

    /**
     * 查询当前用户的 OA 配置；未配置返回 null。
     *
     * @return 配置视图；未配置返回 null
     */
    OaConfigVO getByUserId();

    /**
     * 保存（upsert）当前用户的 OA 配置。
     *
     * @param req 保存请求（password 留空=不修改）
     * @return 保存后的配置视图
     */
    OaConfigVO save(OaConfigSaveReq req);

    /**
     * 查询当前用户的配置实体（含密文），供同步服务解密使用；未配置返回 null。
     * <p>仅内部调用，不对外暴露。
     *
     * @return 配置实体；未配置返回 null
     */
    OaUserConfig loadEntity();

    /**
     * 按用户查询配置实体（含密文）。
     * <p>显式传 userId 以支持无请求上下文的异步场景（OA 同步线程）。
     *
     * @param userId 用户
     * @return 配置实体；未配置返回 null
     */
    OaUserConfig loadByUserId(Long userId);
}
