package com.recall.service.oa.browser;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import com.recall.service.oa.OaContentBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

/**
 * OA「工时日填报」Playwright 操作客户端。
 * <p>
 * 选择器与交互路径均来自 2026-09-14 对致远 A8+ V11.0 的真实浏览器实测：
 * <ul>
 *   <li>登录：main.do → #login_password1 表单；已登录提示「当前已登录了一个用户」时先点「注销」。</li>
 *   <li>找协同：门户待办列表按标题正则（含两个日期段 + GSDA）匹配，OA 周为周六~周五。</li>
 *   <li>表单在 #zwIframe（cap4 引擎），明细行按「填报日期」列定位；行内项目组需点
 *       .cap-icon-mingxibiaoxuanzeqi 图标弹分类菜单，再进 layui「关联列表」弹窗勾选。</li>
 *   <li>文本输入用 native value setter + input 事件（Vue/element-ui 需要）。</li>
 *   <li>收尾：岗位项目属性自查 radio=是 → 点「暂存待办」→ 跳 showDealSuccessfully。</li>
 * </ul>
 * 页面较重偶发崩溃/选择器超时，由上层 {@code OaSyncExecutor} 整轮重试。
 *
 * @author recall
 */
@Slf4j
@Component
public class OaPlaywrightClient {

    /**
     * 执行步骤常量（写同步日志 step，便于失败定位）
     */
    public static final String STEP_LOGIN = "登录OA";
    public static final String STEP_FIND = "查找本周工时协同";
    public static final String STEP_OPEN_FORM = "打开填报表单";
    public static final String STEP_FILL = "填写明细";
    public static final String STEP_RADIO = "勾选岗位项目属性自查";
    public static final String STEP_SAVE = "暂存待办";

    /**
     * 待办标题正则所需片段（标题形如 (自动发起)工时日填报-姓名-2026年9月份第三周-2026-09-12-2026-09-18-GSDA-202609-03787）
     */
    private static final String TITLE_KEYWORD = "工时日填报";

    /**
     * 明细区「新建」按钮文案（半天请假补行用）
     */
    private static final String BTN_NEW_ROW = "新建";

    /**
     * 执行一次完整同步（登录 → 找协同 → 填表 → 暂存）。
     * <p>方法内自建/自毁浏览器实例，任何一步失败抛 {@link OaSyncException}（step 已上报）。
     *
     * @param baseUrl    OA 根地址
     * @param username   OA 账号
     * @param password   OA 密码明文（由上层解密）
     * @param plan       整周填写计划
     * @param headless   是否无头
     * @param stepNotify 步骤回调（更新同步日志 step）
     * @param metaNotify 表单元数据回调（工时提交截止时间/该周是否已填报，读到即回调，供日志持久化）
     */
    public void run(String baseUrl, String username, String password,
                    OaContentBuilder.WeekPlan plan, boolean headless,
                    Consumer<String> stepNotify, Consumer<OaFormMeta> metaNotify) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(headless)
                    .setArgs(List.of("--no-sandbox", "--disable-dev-shm-usage")));
            try {
                BrowserContext context = browser.newContext(new Browser.NewContextOptions().setLocale("zh-CN"));
                Page page = context.newPage();
                page.setDefaultTimeout(30_000);
                // OA 页面注册了 beforeunload 离开确认；Playwright 默认 dismiss（=取消导航），
                // 会阻断暂存后的跳转检测，这里统一自动接受
                context.onDialog(dialog -> {
                    if ("beforeunload".equals(dialog.type())) {
                        dialog.accept();
                    }
                });

                stepNotify.accept(STEP_LOGIN);
                login(page, baseUrl, username, password);

                stepNotify.accept(STEP_FIND);
                Page summaryPage = findCollaboration(page, baseUrl, plan);

                stepNotify.accept(STEP_OPEN_FORM);
                Frame form = openForm(summaryPage);
                // 表单已加载即抓取截止时间与已填报状态（每周固定，暂存成败与否都值得记录）
                metaNotify.accept(readFormMeta(form));

                stepNotify.accept(STEP_FILL);
                fillWeek(summaryPage, form, plan);

                stepNotify.accept(STEP_RADIO);
                checkSelfRadio(form);

                stepNotify.accept(STEP_SAVE);
                savePending(summaryPage);
            } finally {
                browser.close();
            }
        }
    }

    /**
     * 表单元数据：工时提交截止时间 + OA 端该周工时是否已填报。
     *
     * @param deadline  截止时间（表单 field2044，如 2026-09-22 23:59）；读取失败为 null
     * @param submitted 该周工时是否已填报（表单 field2052：是/否）
     */
    public record OaFormMeta(java.time.LocalDateTime deadline, boolean submitted) {
    }

    /**
     * 从表单主表读取截止时间（field2044）与该周工时是否已填报（field2052）
     */
    private OaFormMeta readFormMeta(Frame form) {
        try {
            String raw = (String) form.evaluate("""
                    () => {
                      // 主表字段 class 为 "formmain_6019|field2044"（竖线分隔），明细行才用下划线；
                      // 字段容器内含 label（如"该周工时是否已填报"含'否'字），必须只取值区域
                      const pickVal = (field) => {
                        const el = document.querySelector('[class*="|' + field + '"], [class*="_' + field + '"]');
                        if (!el) return '';
                        const right = el.querySelector('.field__right, .field-content__view, .cap4-ctrl__browse__content');
                        return (right || el).textContent.trim();
                      };
                      return JSON.stringify({ deadline: pickVal('field2044'), submitted: pickVal('field2052') });
                    }
                    """);
            var node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(raw);
            // 截止时间形如 "2026-09-22 23:59"（即使混入 label 文本也能提取）
            LocalDateTime deadline = null;
            var m = java.util.regex.Pattern.compile("(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2})")
                    .matcher(node.path("deadline").asText(""));
            if (m.find()) {
                deadline = LocalDateTime.parse(m.group(1),
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            }
            String submittedText = node.path("submitted").asText("");
            boolean submitted = submittedText.endsWith("是");
            return new OaFormMeta(deadline, submitted);
        } catch (Exception e) {
            log.warn("读取表单元数据（截止时间/已填报状态）失败: {}", e.getMessage());
            return new OaFormMeta(null, false);
        }
    }

    // ===================== 登录 =====================

    private void login(Page page, String baseUrl, String username, String password) {
        OaBrowserSupport.login(page, baseUrl, username, password);
    }

    // ===================== 找协同 =====================

    /**
     * 在门户待办列表中查找本周的工时日填报协同并打开。
     * <p>匹配规则：标题含「工时日填报」且含两个日期段，日期区间须覆盖计划周任一天；
     * 多条时取日期区间起始最大（最新）的一条。
     *
     * @return 协同详情页（弹出的新标签页）
     */
    private Page findCollaboration(Page page, String baseUrl, OaContentBuilder.WeekPlan plan) {
        page.navigate(baseUrl + "/main.do?method=main");
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        List<LocalDate> planDates = plan.days().stream().map(OaContentBuilder.DayFillPlan::date).toList();
        Page popup = null;
        // 门户内容懒加载：轮询最长 30s
        // 注意：Playwright 的 text=/正则/ 只匹配元素自身直接文本，而待办标题在 DOM 中被拆分为多个子节点，
        // 故用宽松 text= 定位候选，日期覆盖关系在 Java 侧用正则校验。
        for (int i = 0; i < 15 && popup == null; i++) {
            Locator candidates = page.locator("text=" + TITLE_KEYWORD);
            int n = candidates.count();
            ElementHandle target = null;
            String bestStart = "";
            for (int c = 0; c < n; c++) {
                Locator cand = candidates.nth(c);
                String text;
                try {
                    text = cand.innerText();
                } catch (Exception e) {
                    continue; // 元素可能刚被重渲染
                }
                // 排除消息列表里的引用行（《标题》包裹），只认待办/公告里的裸标题
                if (text.contains("《")) {
                    continue;
                }
                java.util.regex.Matcher m = java.util.regex.Pattern
                        .compile("(\\d{4}-\\d{2}-\\d{2})-(\\d{4}-\\d{2}-\\d{2})-GSDA").matcher(text);
                if (!m.find()) {
                    continue;
                }
                LocalDate start = LocalDate.parse(m.group(1));
                LocalDate end = LocalDate.parse(m.group(2));
                boolean covers = planDates.stream().anyMatch(d -> !d.isBefore(start) && !d.isAfter(end));
                if (covers && m.group(1).compareTo(bestStart) >= 0) {
                    bestStart = m.group(1);
                    target = cand.elementHandle();
                }
            }
            if (target != null) {
                final ElementHandle clickTarget = target;
                popup = page.context().waitForPage(
                        new BrowserContext.WaitForPageOptions().setTimeout(15_000),
                        () -> clickTarget.dispatchEvent("click"));
                break;
            }
            page.waitForTimeout(2_000);
        }
        if (popup == null) {
            debugSnapshot(page, "find-collaboration");
            throw new OaSyncException("本周 OA 工时日填报协同未发起，或不在待办列表内，请先在 OA 确认后重试");
        }
        popup.waitForLoadState(LoadState.DOMCONTENTLOADED);
        return popup;
    }

    // ===================== 打开表单 =====================

    /**
     * 等待 cap4 表单 iframe 就绪并返回 Frame（轮询明细行字段出现，总超时约 60s）
     */
    private Frame openForm(Page summaryPage) {
        summaryPage.waitForSelector("#zwIframe", new Page.WaitForSelectorOptions().setTimeout(30_000));
        for (int i = 0; i < 30; i++) {
            Frame form = summaryPage.frames().stream()
                    .filter(f -> f.url().contains("/cap4/"))
                    .findFirst().orElse(null);
            if (form != null && Boolean.TRUE.equals(form.evaluate(
                    "() => !!document.querySelector('[class*=\"formson_6021_\"][class*=\"_field1799\"]')"))) {
                return form;
            }
            summaryPage.waitForTimeout(2_000);
        }
        throw new OaSyncException("工时填报表单加载超时");
    }

    // ===================== 填写 =====================

    private void fillWeek(Page summaryPage, Frame form, OaContentBuilder.WeekPlan plan) {
        for (OaContentBuilder.DayFillPlan day : plan.days()) {
            List<Locator> rows = rowsForDate(form, day.date());
            for (int r = 0; r < day.rows().size(); r++) {
                OaContentBuilder.FillRow row = day.rows().get(r);
                Locator tr = r < rows.size() ? rows.get(r) : ensureExtraRow(summaryPage, form, day.date(), rows.size());
                fillRow(summaryPage, form, tr, row);
            }
        }
    }

    /**
     * 找出「填报日期」等于指定日期的明细行（可能为 0/1/2 行）
     */
    private List<Locator> rowsForDate(Frame form, LocalDate date) {
        Locator allRows = form.locator("tr:has([class*='_field1797'])");
        int n = allRows.count();
        java.util.List<Locator> matched = new java.util.ArrayList<>();
        for (int i = 0; i < n; i++) {
            Locator tr = allRows.nth(i);
            String text = tr.locator("[class*='_field1797']").first().textContent();
            if (text != null && text.contains(date.toString())) {
                matched.add(tr);
            }
        }
        return matched;
    }

    /**
     * 半天请假需要第二天：点明细区「新建」补一行，并把新行填报日期改为目标日期。
     * <p>新行通常是表尾日期为空/默认的那行，通过行数对比定位。
     */
    private Locator ensureExtraRow(Page summaryPage, Frame form, LocalDate date, int beforeCount) {
        form.getByText(BTN_NEW_ROW, new Frame.GetByTextOptions().setExact(true)).first().click();
        summaryPage.waitForTimeout(1_500);
        List<Locator> rows = rowsForDate(form, date);
        if (rows.size() > beforeCount) {
            return rows.get(rows.size() - 1);
        }
        // 新行日期非目标值 → 找一条多余行并改日期
        Locator allRows = form.locator("tr:has([class*='_field1797'])");
        int after = allRows.count();
        for (int i = after - 1; i >= 0; i--) {
            Locator tr = allRows.nth(i);
            String text = tr.locator("[class*='_field1797']").first().textContent();
            if (text == null || text.isBlank() || !text.contains("2026") && !text.contains("202")) {
                setDate(form, tr, date);
                return tr;
            }
        }
        // 兜底：取最后一行改日期
        Locator last = allRows.nth(after - 1);
        setDate(form, last, date);
        return last;
    }

    /**
     * 设置行的填报日期（cap4 日期控件内输入框，native setter + input/change 事件）
     */
    private void setDate(Frame form, Locator tr, LocalDate date) {
        ElementHandle input = tr.locator("[class*='_field1797'] input").first().elementHandle();
        if (input == null) {
            throw new OaSyncException("补行的填报日期输入框未找到，无法完成半天请假补行");
        }
        form.evaluate("""
                ([el, v]) => {
                  const s = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set;
                  s.call(el, v);
                  el.dispatchEvent(new Event('input', { bubbles: true }));
                  el.dispatchEvent(new Event('change', { bubbles: true }));
                }
                """, List.of(input, date.toString()));
    }

    /**
     * 填一行：项目组（分类菜单 + 关联列表弹窗）→ 工作内容 → 工时占比。
     * 选完项目组 OA 会自动带出部门/编号/负责人，无需处理。
     */
    private void fillRow(Page summaryPage, Frame form, Locator tr, OaContentBuilder.FillRow row) {
        selectProject(summaryPage, form, tr, row.projectName(), row.leave());
        if (row.content() != null && !row.content().isBlank()) {
            ElementHandle ta = tr.locator("textarea.ui-textarea__inner").first().elementHandle();
            if (ta != null) {
                form.evaluate("""
                        ([el, v]) => {
                          const s = Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype, 'value').set;
                          s.call(el, v);
                          el.dispatchEvent(new Event('input', { bubbles: true }));
                          el.dispatchEvent(new Event('change', { bubbles: true }));
                        }
                        """, List.of(ta, row.content()));
            }
        }
        ElementHandle num = tr.locator("[class*='_field0985'] input.ui-input__inner").first().elementHandle();
        if (num == null) {
            num = tr.locator("section.cap4-number input.ui-input__inner").first().elementHandle();
        }
        if (num != null) {
            String ratio = BigDecimal.valueOf(row.ratio()).stripTrailingZeros().toPlainString();
            form.evaluate("""
                    ([el, v]) => {
                      const s = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set;
                      s.call(el, v);
                      el.dispatchEvent(new Event('input', { bubbles: true }));
                      el.dispatchEvent(new Event('change', { bubbles: true }));
                      el.dispatchEvent(new FocusEvent('blur'));
                    }
                    """, List.of(num, ratio));
        }
    }

    /**
     * 项目组选择：点行内选择器图标 → 分类菜单（选择研发项目/选择请假项目）→
     * layui「关联列表」弹窗按名称勾选 → 弹窗「确定」。
     */
    private void selectProject(Page summaryPage, Frame form, Locator tr, String projectName, boolean leave) {
        // 1. 激活行（首次点击进入编辑态）
        tr.locator("[class*='_field1799']").first().click();
        summaryPage.waitForTimeout(800);
        // 2. 点选择器图标弹分类菜单（实测需 mousedown + click 组合）
        ElementHandle icon = tr.locator("[class*='_field1799'] .cap-icon-mingxibiaoxuanzeqi")
                .first().elementHandle();
        if (icon == null) {
            throw new OaSyncException("项目组选择器图标未找到");
        }
        form.evaluate("""
                (el) => {
                  el.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }));
                  el.click();
                }
                """, icon);
        summaryPage.waitForTimeout(1_500);
        // 3. 点分类
        String category = leave ? "选择请假项目" : "选择研发项目";
        Boolean clicked = (Boolean) form.evaluate("""
                (cat) => {
                  const li = [...document.querySelectorAll('li.selectForm-list__item')]
                          .find(e => e.textContent.trim() === cat);
                  if (!li) return false;
                  li.click();
                  return true;
                }
                """, category);
        if (!Boolean.TRUE.equals(clicked)) {
            throw new OaSyncException("项目组分类菜单未找到「" + category + "」"
                    + (leave ? "（当天可能无请假记录，请假项目不可选）" : ""));
        }
        // 4. 等 layui「关联列表」弹窗出现
        Frame dlg = null;
        for (int i = 0; i < 15; i++) {
            summaryPage.waitForTimeout(1_000);
            dlg = summaryPage.frames().stream()
                    .filter(f -> f.name() != null && f.name().contains("layui-layer-iframe"))
                    .findFirst().orElse(null);
            if (dlg != null && dlg.locator("tr").count() > 0) {
                break;
            }
        }
        if (dlg == null) {
            throw new OaSyncException("项目选择弹窗未打开");
        }
        // 5. 按项目名勾选
        Locator targetRow = dlg.locator("tr:has-text('" + projectName + "')").first();
        if (targetRow.count() == 0) {
            if (leave) {
                // OA 的「选择请假项目」列表来自 OA 请假流程（审批以 OA 为准），
                // OA 无该日请假流程时列表为空——平台记录了请假但 OA 没有时的明确提示
                throw new OaSyncException("OA 请假项目列表中未找到「" + projectName + "」"
                        + "：OA 中该日没有请假流程记录（选项仅当天在 OA 存在请假时才出现），"
                        + "请先在 OA 发起请假流程后再同步");
            }
            throw new OaSyncException("项目选择弹窗中未找到「" + projectName + "」，"
                    + "请确认项目名与 OA「选择研发项目」列表中的名称一致（含括号全角/半角）");
        }
        Locator check = targetRow.locator("input[type='checkbox'], input[type='radio']").first();
        if (check.count() > 0) {
            check.click();
        } else {
            targetRow.click();
        }
        summaryPage.waitForTimeout(800);
        // 6. 弹窗「确定」
        summaryPage.getByText("确定", new Page.GetByTextOptions().setExact(true)).first().click();
        summaryPage.waitForTimeout(2_000);
        // 7. 校验回填
        String filled = tr.locator("[class*='_field1799']").first().textContent();
        if (filled == null || !filled.contains(projectName)) {
            throw new OaSyncException("项目组「" + projectName + "」回填校验失败");
        }
    }

    // ===================== 收尾 =====================

    /**
     * 表头「岗位项目属性自查」radio 选「是」
     */
    private void checkSelfRadio(Frame form) {
        String result = (String) form.evaluate("""
                () => {
                  const radios = [...document.querySelectorAll('input[type=radio]')];
                  const yes = radios.find(r => {
                    let e = r;
                    for (let i = 0; i < 5 && e; i++, e = e.parentElement) {
                      const t = (e.textContent || '').trim();
                      if (t === '是') return true;
                    }
                    return false;
                  });
                  if (!yes) return 'not-found';
                  if (!yes.checked) yes.click();
                  return yes.checked ? 'checked' : 'unchecked';
                }
                """);
        if (!"checked".equals(result)) {
            throw new OaSyncException("岗位项目属性自查 radio 勾选失败: " + result);
        }
    }

    /**
     * 点「暂存待办」。
     * <p>成功信号二选一：跳转到操作成功页，或 OA 保存后自动关闭 window.open 弹出的协同窗口
     * （实测暂存成功后 Seeyon 会自关窗口刷新父页，此时页面引用即失效，同样视为成功）。
     */
    private void savePending(Page summaryPage) {
        summaryPage.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("暂存待办")).click();
        try {
            summaryPage.waitForURL("**/showDealSuccessfully**", new Page.WaitForURLOptions().setTimeout(60_000));
        } catch (PlaywrightException e) {
            if (summaryPage.isClosed()) {
                log.info("暂存后 OA 自动关闭了协同窗口，视为暂存成功");
                return;
            }
            throw e;
        }
    }

    /**
     * 同步业务异常（消息直接展示给用户）
     */
    public static class OaSyncException extends RuntimeException {
        public OaSyncException(String message) {
            super(message);
        }
    }

    /**
     * 失败现场截图（写入系统临时目录，便于排查选择器问题）
     */
    private void debugSnapshot(Page page, String tag) {
        try {
            String file = System.getProperty("java.io.tmpdir") + "/oa-sync-" + tag + "-" + System.currentTimeMillis() + ".png";
            page.screenshot(new Page.ScreenshotOptions().setPath(java.nio.file.Paths.get(file))
                    .setFullPage(true));
            log.info("OA 同步调试截图: {}", file);
        } catch (Exception e) {
            log.warn("调试截图失败: {}", e.getMessage());
        }
    }
}
