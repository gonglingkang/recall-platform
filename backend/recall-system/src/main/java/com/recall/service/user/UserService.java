package com.recall.service.user;

import com.recall.dto.user.PasswordReq;
import com.recall.dto.user.PreferencesReq;
import com.recall.dto.user.ProfileReq;
import com.recall.vo.auth.UserVO;

/**
 * 用户设置 Service（PRD 6.9 / 11.6）。
 *
 * @author recall
 */
public interface UserService {

    /**
     * 修改用户资料。
     *
     * @param req 资料修改请求
     * @return 更新后的用户信息
     */
    UserVO updateProfile(ProfileReq req);

    /**
     * 修改密码（需校验原密码）。
     *
     * @param req 密码修改请求
     */
    void updatePassword(PasswordReq req);

    /**
     * 修改用户偏好设置。
     *
     * @param req 偏好修改请求
     * @return 更新后的用户信息
     */
    UserVO updatePreferences(PreferencesReq req);
}
