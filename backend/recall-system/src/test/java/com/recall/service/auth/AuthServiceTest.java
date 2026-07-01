package com.recall.service.auth;

import com.recall.BaseTest;
import com.recall.common.api.ResultCode;
import com.recall.common.exception.BusinessException;
import com.recall.dto.auth.LoginReq;
import com.recall.dto.auth.RegisterReq;
import com.recall.vo.auth.LoginVO;
import com.recall.vo.auth.UserVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 认证业务逻辑测试（PRD 6.2）。
 *
 * @author recall
 */
class AuthServiceTest extends BaseTest {

    @Autowired
    private AuthService authService;

    @Test
    void register_shouldCreateUserAndDefaultCategoriesAndReturnToken() {
        String name = "al" + unique();
        LoginVO vo = authService.register(newReq(name, name + "@t.com"));

        assertNotNull(vo.getToken(), "注册应返回 JWT");
        assertNotNull(vo.getUserId());
        assertEquals(name, vo.getUsername());
    }

    @Test
    void register_duplicateUsername_shouldThrow() {
        String name = "du" + unique();
        authService.register(newReq(name, name + "@t.com"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.register(newReq(name, "other" + unique() + "@t.com")));
        assertEquals(ResultCode.USERNAME_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void register_passwordMismatch_shouldThrow() {
        RegisterReq req = newReq("mm" + unique(), "mm@t.com");
        req.setConfirmPassword("Different1");
        BusinessException ex = assertThrows(BusinessException.class, () -> authService.register(req));
        assertEquals(ResultCode.PARAM_VALIDATE_FAILED.getCode(), ex.getCode());
    }

    @Test
    void login_withUsername_shouldSucceed() {
        String name = "bo" + unique();
        authService.register(newReq(name, name + "@t.com"));

        LoginReq login = new LoginReq();
        login.setAccount(name);
        login.setPassword("Pass1234");
        LoginVO vo = authService.login(login);

        assertNotNull(vo.getToken());
        assertEquals(name, vo.getUsername());
    }

    @Test
    void login_withEmail_shouldSucceed() {
        String name = "ca" + unique();
        String email = name + "@t.com";
        authService.register(newReq(name, email));

        LoginReq login = new LoginReq();
        login.setAccount(email);
        login.setPassword("Pass1234");
        LoginVO vo = authService.login(login);
        assertNotNull(vo.getToken());
    }

    @Test
    void login_wrongPassword_shouldThrowLoginFailed() {
        String name = "da" + unique();
        authService.register(newReq(name, name + "@t.com"));

        LoginReq login = new LoginReq();
        login.setAccount(name);
        login.setPassword("WrongPass1");
        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(login));
        assertEquals(ResultCode.LOGIN_FAILED.getCode(), ex.getCode());
    }

    @Test
    void currentUser_shouldReturnLoggedInUser() {
        Long userId = loginAsNewUser();
        UserVO vo = authService.currentUser();
        assertEquals(userId, vo.getId());
    }

    private RegisterReq newReq(String username, String email) {
        RegisterReq req = new RegisterReq();
        req.setUsername(username);
        req.setEmail(email);
        req.setPassword("Pass1234");
        req.setConfirmPassword("Pass1234");
        return req;
    }
}
