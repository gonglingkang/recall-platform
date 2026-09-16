package com.recall.service.oa.browser;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import lombok.extern.slf4j.Slf4j;

/**
 * OA 浏览器公共操作（登录）。
 * <p>
 * 供工时同步与考勤抓取共用；登录流程来自对致远 A8+ V11.0 实测：
 * main.do 登录表单 → 残留会话提示「当前已登录了一个用户」先点「注销」。
 *
 * @author recall
 */
@Slf4j
public final class OaBrowserSupport {

    private OaBrowserSupport() {
    }

    /**
     * 登录 OA（会话有效时跳过）。
     *
     * @param page     页面（登录后停在 main.do?method=main）
     * @param baseUrl  OA 根地址
     * @param username 账号
     * @param password 密码明文
     */
    public static void login(Page page, String baseUrl, String username, String password) {
        page.navigate(baseUrl + "/main.do");
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        // 残留会话提示：同一账号已在线 → 点「注销」重新登录
        Locator reloginTip = page.getByText("当前已登录了一个用户");
        if (reloginTip.first().isVisible(new Locator.IsVisibleOptions().setTimeout(3_000))) {
            page.getByText("注销", new Page.GetByTextOptions().setExact(true)).first().click();
            log.info("OA 检测到残留登录会话，已注销重登");
        }
        Locator pwd = page.locator("#login_password1");
        if (pwd.isVisible(new Locator.IsVisibleOptions().setTimeout(5_000))) {
            page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("账号/手机号"))
                    .fill(username);
            pwd.fill(password);
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("登 录")).click();
            page.waitForURL("**/main.do?method=main", new Page.WaitForURLOptions().setTimeout(30_000));
        } else {
            log.info("OA 会话有效，跳过登录表单");
        }
    }
}
