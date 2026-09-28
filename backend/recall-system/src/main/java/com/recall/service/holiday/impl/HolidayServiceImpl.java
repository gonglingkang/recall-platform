package com.recall.service.holiday.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recall.dao.holiday.HolidayConfigMapper;
import com.recall.entity.holiday.HolidayConfig;
import com.recall.enums.HolidayType;
import com.recall.service.holiday.HolidayService;
import com.recall.vo.holiday.HolidayDayVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 法定节假日/补班 Service 实现。
 * <p>
 * Mapper 唯一归属本 Service；外部 API 超时/异常/空数据时降级内置兜底表。
 *
 * @author recall
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HolidayServiceImpl implements HolidayService {

    private static final String API_URL = "https://timor.tech/api/holiday/year/";

    /** 内置兜底（2025/2026 国务院安排；仅外部 API 不可用时使用） */
    private static final Map<Integer, Map<Integer, List<String>>> FALLBACK = new HashMap<>();

    static {
        FALLBACK.computeIfAbsent(2026, y -> new HashMap<>())
                .computeIfAbsent(HolidayType.HOLIDAY.getCode(), t -> new ArrayList<>())
                .addAll(List.of("2026-01-01",
                        "2026-01-28", "2026-01-29", "2026-01-30", "2026-01-31",
                        "2026-02-01", "2026-02-02", "2026-02-03", "2026-02-04",
                        "2026-04-04", "2026-04-05", "2026-04-06",
                        "2026-05-01", "2026-05-02", "2026-05-03", "2026-05-04", "2026-05-05",
                        "2026-06-19", "2026-06-20", "2026-06-21",
                        "2026-10-01", "2026-10-02", "2026-10-03", "2026-10-04",
                        "2026-10-05", "2026-10-06", "2026-10-07", "2026-10-08"));
        FALLBACK.computeIfAbsent(2026, y -> new HashMap<>())
                .computeIfAbsent(HolidayType.MAKEUP.getCode(), t -> new ArrayList<>())
                .addAll(List.of("2026-01-25", "2026-02-08", "2026-04-26", "2026-05-09", "2026-09-20", "2026-10-10"));
        FALLBACK.computeIfAbsent(2025, y -> new HashMap<>())
                .computeIfAbsent(HolidayType.HOLIDAY.getCode(), t -> new ArrayList<>())
                .addAll(List.of("2025-01-01",
                        "2025-01-28", "2025-01-29", "2025-01-30", "2025-01-31",
                        "2025-02-01", "2025-02-02", "2025-02-03", "2025-02-04",
                        "2025-04-04", "2025-04-05", "2025-04-06",
                        "2025-05-01", "2025-05-02", "2025-05-03", "2025-05-04", "2025-05-05",
                        "2025-05-31", "2025-06-01", "2025-06-02",
                        "2025-10-01", "2025-10-02", "2025-10-03", "2025-10-04",
                        "2025-10-05", "2025-10-06", "2025-10-07", "2025-10-08"));
        FALLBACK.computeIfAbsent(2025, y -> new HashMap<>())
                .computeIfAbsent(HolidayType.MAKEUP.getCode(), t -> new ArrayList<>())
                .addAll(List.of("2025-01-26", "2025-02-08", "2025-04-27", "2025-05-10", "2025-09-28", "2025-10-11"));
    }

    private final HolidayConfigMapper holidayConfigMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<HolidayDayVO> getYear(int year) {
        List<HolidayConfig> cached = holidayConfigMapper.selectList(new LambdaQueryWrapper<HolidayConfig>()
                .eq(HolidayConfig::getYear, year)
                .orderByAsc(HolidayConfig::getHolidayDate));
        if (!cached.isEmpty()) {
            return cached.stream().map(c -> new HolidayDayVO(c.getHolidayDate(), c.getType(), c.getName())).toList();
        }
        List<HolidayDayVO> fetched = fetchFromApi(year);
        if (fetched.isEmpty()) {
            log.warn("节假日 API 拉取失败/为空，使用内置兜底: year={}", year);
            return fallback(year);
        }
        cacheYear(year, fetched);
        return fetched;
    }

    /** 从 timor API 拉取并解析；响应结构 { code:0, holiday: { "MM-dd": {holiday:true/false, name:..} } } */
    private List<HolidayDayVO> fetchFromApi(int year) {
        try {
            String body = RestClient.create()
                    .get()
                    .uri(API_URL + year)
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(body);
            JsonNode holidayMap = root.path("holiday");
            if (!holidayMap.isObject() || holidayMap.isEmpty()) {
                return List.of();
            }
            List<HolidayDayVO> result = new ArrayList<>();
            holidayMap.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode item = entry.getValue();
                boolean isHoliday = item.path("holiday").asBoolean(false);
                String name = item.path("name").asText(null);
                LocalDate date = LocalDate.parse(year + "-" + key);
                result.add(new HolidayDayVO(date,
                        isHoliday ? HolidayType.HOLIDAY.getCode() : HolidayType.MAKEUP.getCode(), name));
            });
            result.sort(Comparator.comparing(HolidayDayVO::getDate));
            log.info("节假日 API 拉取成功: year={}, 条数={}", year, result.size());
            return result;
        } catch (Exception e) {
            log.warn("节假日 API 拉取异常: year={}, err={}", year, e.getMessage());
            return List.of();
        }
    }

    /** 缓存入库（先清后插；入库失败仅告警，不影响返回） */
    private void cacheYear(int year, List<HolidayDayVO> data) {
        try {
            holidayConfigMapper.delete(new LambdaQueryWrapper<HolidayConfig>()
                    .eq(HolidayConfig::getYear, year));
            data.forEach(d -> {
                HolidayConfig config = new HolidayConfig();
                config.setYear(year);
                config.setHolidayDate(d.getDate());
                config.setType(d.getType());
                config.setName(d.getName());
                holidayConfigMapper.insert(config);
            });
        } catch (Exception e) {
            log.warn("节假日缓存入库失败: year={}, err={}", year, e.getMessage());
        }
    }

    private List<HolidayDayVO> fallback(int year) {
        Map<Integer, List<String>> byType = FALLBACK.get(year);
        if (byType == null) {
            return List.of();
        }
        List<HolidayDayVO> result = new ArrayList<>();
        byType.forEach((type, dates) -> dates.forEach(d ->
                result.add(new HolidayDayVO(LocalDate.parse(d), type, type == HolidayType.HOLIDAY.getCode() ? "法定节假日" : "补班日"))));
        result.sort(Comparator.comparing(HolidayDayVO::getDate));
        return result;
    }
}
