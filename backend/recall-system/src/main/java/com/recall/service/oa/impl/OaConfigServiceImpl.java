package com.recall.service.oa.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recall.common.context.UserContextHolder;
import com.recall.common.exception.BusinessException;
import com.recall.common.api.ResultCode;
import com.recall.common.util.AesCipher;
import com.recall.dao.oa.OaUserConfigMapper;
import com.recall.dto.oa.OaConfigSaveReq;
import com.recall.entity.oa.OaUserConfig;
import com.recall.service.oa.OaConfigService;
import com.recall.vo.oa.OaConfigVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * OA 对接配置 Service 实现。
 * <p>
 * Mapper 唯一归属本 Service；同步服务经 {@link #loadEntity()} 取实体。
 *
 * @author recall
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OaConfigServiceImpl implements OaConfigService {

    private final OaUserConfigMapper oaUserConfigMapper;

    @Value("${recall.oa.crypto-key}")
    private String cryptoKey;

    @Override
    public OaConfigVO getByUserId() {
        OaUserConfig config = loadEntity();
        if (config == null) {
            return null;
        }
        return OaConfigVO.builder()
                .oaBaseUrl(config.getOaBaseUrl())
                .username(config.getUsername())
                .hasPassword(true)
                .devProjectName(config.getDevProjectName())
                .leaveProjectName(config.getLeaveProjectName())
                .autoSync(Boolean.TRUE.equals(config.getAutoSync()))
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OaConfigVO save(OaConfigSaveReq req) {
        Long userId = UserContextHolder.requireUserId();
        OaUserConfig config = loadEntity();
        boolean creating = config == null;
        if (creating) {
            config = new OaUserConfig();
            config.setUserId(userId);
        }
        if (creating && isBlank(req.getPassword())) {
            throw new BusinessException(ResultCode.OA_CONFIG_INCOMPLETE, "首次配置必须填写 OA 密码");
        }
        config.setOaBaseUrl(trimTrailingSlash(req.getOaBaseUrl().trim()));
        config.setUsername(req.getUsername().trim());
        if (!isBlank(req.getPassword())) {
            config.setPasswordCipher(AesCipher.encrypt(req.getPassword(), cryptoKey));
        }
        config.setDevProjectName(req.getDevProjectName().trim());
        config.setLeaveProjectName(req.getLeaveProjectName().trim());
        config.setAutoSync(Boolean.TRUE.equals(req.getAutoSync()));
        if (creating) {
            oaUserConfigMapper.insert(config);
        } else {
            oaUserConfigMapper.updateById(config);
        }
        log.info("保存OA配置: userId={}, autoSync={}", userId, config.getAutoSync());
        return getByUserId();
    }

    @Override
    public OaUserConfig loadEntity() {
        return loadByUserId(UserContextHolder.requireUserId());
    }

    @Override
    public OaUserConfig loadByUserId(Long userId) {
        List<OaUserConfig> list = oaUserConfigMapper.selectList(new LambdaQueryWrapper<OaUserConfig>()
                .eq(OaUserConfig::getUserId, userId));
        return list.isEmpty() ? null : list.get(0);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
