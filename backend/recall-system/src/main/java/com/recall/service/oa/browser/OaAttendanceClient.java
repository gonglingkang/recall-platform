package com.recall.service.oa.browser;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.LoadState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * OA「考勤打卡记录」Playwright 抓取客户端。
 * <p>
 * 交互路径来自 2026-09-15 对致远 A8+ V11.0 报表页实测（report4Result.do，designId 固定可深链）：
 * <ul>
 *   <li>报表在 cap4/report iframe；「打卡日期」为 ant-design RangePicker，起止 input 可直接键入日期
 *       （任意月份，无需日历导航，天然规避跨月翻页）→ 点「筛选」→ 表格精确出该日记录。</li>
 *   <li>行 12 列：姓名/人员编号/打卡日期/日期类型/所属部门/所属大区/上班卡/下班卡/考勤结果/出勤状态/当天是否请假/当天是否出差。</li>
 *   <li>休息日行存在但打卡为空；该日完全无行 = OA 数据尚未生成（次日补抓）。</li>
 * </ul>
 *
 * @author recall
 */
@Slf4j
@Component
public class OaAttendanceClient {

    /** 考勤打卡记录报表深链（报表模板 designId，环境固定） */
    private static final String REPORT_PATH = "/report4Result.do?method=showResult&designId=7330452547660957853&mobile=0";

    private static final DateTimeFormatter DAY = DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * 单日考勤行。
     *
     * @param workDate         考勤日期
     * @param dateType         日期类型（工作日/休息日）
     * @param clockIn          上班卡 HH:mm:ss（休息日为空）
     * @param clockOut         下班卡 HH:mm:ss
     * @param attendanceResult 考勤结果（正常/异常）
     * @param attendanceStatus 出勤状态原文（迟到8分钟/正常出勤/缺勤7.50小时…）
     * @param onLeave          当天是否请假
     */
    public record AttendanceRow(LocalDate workDate, String dateType, String clockIn, String clockOut,
                                String attendanceResult, String attendanceStatus, boolean onLeave) {
    }

    /**
     * 抓取某天的考勤打卡记录（便捷方法，见 {@link #fetchDays}）。
     *
     * @return 当天考勤行；OA 无该日数据（未生成/无考勤）返回 empty
     */
    public Optional<AttendanceRow> fetchDay(String baseUrl, String username, String password,
                                            LocalDate date, boolean headless) {
        return fetchDays(baseUrl, username, password, List.of(date), headless)
                .values().stream().findFirst();
    }

    /**
     * 抓取多天的考勤打卡记录（同一浏览器会话内逐日筛选，避免重复登录互踢）。
     *
     * @param baseUrl  OA 根地址
     * @param username 账号
     * @param password 密码明文
     * @param dates    考勤日期列表
     * @param headless 是否无头
     * @return 日期 -> 考勤行；OA 无该日数据（未生成/无考勤）不在 Map 中
     */
    public java.util.Map<LocalDate, AttendanceRow> fetchDays(String baseUrl, String username, String password,
                                                             List<LocalDate> dates, boolean headless) {
        java.util.Map<LocalDate, AttendanceRow> result = new java.util.HashMap<>();
        if (dates == null || dates.isEmpty()) {
            return result;
        }
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(headless)
                    .setArgs(List.of("--no-sandbox", "--disable-dev-shm-usage")));
            try {
                BrowserContext context = browser.newContext(new Browser.NewContextOptions().setLocale("zh-CN"));
                Page page = context.newPage();
                page.setDefaultTimeout(30_000);
                context.onDialog(dialog -> {
                    if ("beforeunload".equals(dialog.type())) {
                        dialog.accept();
                    }
                });

                OaBrowserSupport.login(page, baseUrl, username, password);
                page.navigate(baseUrl + REPORT_PATH);
                page.waitForLoadState(LoadState.DOMCONTENTLOADED);
                Frame report = waitForReportFrame(page);

                for (LocalDate date : dates) {
                    try {
                        filterByDay(report, date);
                        readRow(report, date).ifPresent(row -> result.put(date, row));
                    } catch (Exception e) {
                        log.warn("OA 考勤单日读取失败: date={}, err={}", date, e.getMessage());
                    }
                }
            } finally {
                browser.close();
            }
        }
        return result;
    }

    /** 等待 cap4/report 报表 iframe 就绪 */
    private Frame waitForReportFrame(Page page) {
        for (int i = 0; i < 30; i++) {
            Frame frame = page.frames().stream()
                    .filter(f -> f.url().contains("/cap4/report/"))
                    .findFirst().orElse(null);
            if (frame != null && Boolean.TRUE.equals(frame.evaluate(
                    "() => !!document.querySelector('.ant-calendar-range-picker-input')"))) {
                return frame;
            }
            page.waitForTimeout(2_000);
        }
        throw new OaPlaywrightClient.OaSyncException("考勤打卡记录页面加载超时");
    }

    /** 把日期筛选设为指定单日并点「筛选」 */
    private void filterByDay(Frame report, LocalDate date) {
        String day = date.format(DAY);
        Locator start = report.locator(".ant-calendar-range-picker-input").nth(0);
        start.click();
        report.waitForTimeout(1_000);
        start.fill(day);
        report.waitForTimeout(500);
        start.press("Enter");
        report.waitForTimeout(1_000);

        Locator end = report.locator(".ant-calendar-range-picker-input").nth(1);
        end.click();
        report.waitForTimeout(800);
        end.fill(day);
        report.waitForTimeout(500);
        end.press("Enter");
        report.waitForTimeout(1_000);

        report.getByText("筛选", new Frame.GetByTextOptions().setExact(true)).first().click();
        report.waitForTimeout(3_500);
    }

    /** 读取指定日期的表格行 */
    private Optional<AttendanceRow> readRow(Frame report, LocalDate date) {
        String day = date.format(DAY);
        Locator rows = report.locator("table tr:has(td)");
        int n = rows.count();
        for (int i = 0; i < n; i++) {
            String rowText = rows.nth(i).innerText();
            if (rowText == null || !rowText.contains(day)) {
                continue;
            }
            List<String> cells = rows.nth(i).locator("td").allInnerTexts();
            if (cells.size() < 11) {
                continue;
            }
            // 列序：0姓名 1人员编号 2打卡日期 3日期类型 4部门 5大区 6上班卡 7下班卡 8考勤结果 9出勤状态 10当天是否请假
            String clockIn = blankIfEmpty(cells.get(6));
            String clockOut = blankIfEmpty(cells.get(7));
            String status = blankIfEmpty(cells.get(9));
            String result = blankIfEmpty(cells.get(8));
            boolean onLeave = "是".equals(blankIfEmpty(cells.get(10)));
            log.info("OA 考勤行读取成功: date={}, type={}, in={}, out={}, status={}",
                    day, cells.get(3).trim(), clockIn, clockOut, status);
            return Optional.of(new AttendanceRow(date, cells.get(3).trim(), clockIn, clockOut, result, status, onLeave));
        }
        return Optional.empty();
    }

    private static String blankIfEmpty(String text) {
        return text == null ? "" : text.replace("\n", "").trim();
    }
}
