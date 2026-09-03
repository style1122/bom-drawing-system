package com.bom.service;

import com.bom.mapper.DrawingMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 仪表盘统计：图纸上传量与存储占用。
 * 统计窗口统一为「近 N 天（含今天）」，缺失日期补 0；
 * 存储增长趋势按累计字节计算，并包含窗口开始前的历史基线，使曲线连续。
 */
@Service
public class DashboardService {

    @Autowired
    private DrawingMapper drawingMapper;

    private static final int DEFAULT_DAYS = 30;

    public Map<String, Object> getDrawingStats() {
        return getDrawingStats(DEFAULT_DAYS);
    }

    public Map<String, Object> getDrawingStats(int days) {
        Map<String, Object> data = new HashMap<>();

        // 今日上传数量
        data.put("todayUploadCount", drawingMapper.countToday());

        // 图纸文件总占用空间（字节）
        long totalBytes = drawingMapper.sumTotalSize();
        data.put("totalStorageBytes", totalBytes);

        // 近 days 天每日上传数量（日期补齐）
        List<Map<String, Object>> dailyRaw = drawingMapper.countByDay(days);
        data.put("dailyUploadList", fillDaily(dailyRaw, days));

        // 存储增长趋势：累计字节（含窗口前基线）
        List<Map<String, Object>> sizeRaw = drawingMapper.sumSizeByDay(days);
        long windowSum = 0L;
        for (Map<String, Object> r : sizeRaw) {
            windowSum += ((Number) r.get("total_size")).longValue();
        }
        long baseline = Math.max(totalBytes - windowSum, 0L);
        data.put("storageTrendList", fillCumulative(sizeRaw, days, baseline));

        // 图纸类型数量对比：PDF 图纸 / 三维图纸 / 工程图纸
        Map<String, Object> cat = drawingMapper.countByCategory();
        data.put("pdfCount", ((Number) cat.get("pdf_count")).longValue());
        data.put("model3dCount", ((Number) cat.get("model3d_count")).longValue());
        data.put("engineeringCount", ((Number) cat.get("engineering_count")).longValue());

        return data;
    }

    private List<Map<String, Object>> fillDaily(List<Map<String, Object>> raw, int days) {
        Map<String, Integer> map = new HashMap<>();
        for (Map<String, Object> r : raw) {
            map.put(formatDate(r.get("stat_date")), ((Number) r.get("cnt")).intValue());
        }
        List<Map<String, Object>> out = new ArrayList<>();
        LocalDate end = LocalDate.now();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate d = end.minusDays(i);
            String ds = d.format(DateTimeFormatter.ISO_LOCAL_DATE);
            Map<String, Object> m = new HashMap<>();
            m.put("date", ds);
            m.put("count", map.getOrDefault(ds, 0));
            out.add(m);
        }
        return out;
    }

    private List<Map<String, Object>> fillCumulative(List<Map<String, Object>> raw, int days, long baseline) {
        Map<String, Long> map = new HashMap<>();
        for (Map<String, Object> r : raw) {
            map.put(formatDate(r.get("stat_date")), ((Number) r.get("total_size")).longValue());
        }
        List<Map<String, Object>> out = new ArrayList<>();
        LocalDate end = LocalDate.now();
        long running = baseline;
        for (int i = days - 1; i >= 0; i--) {
            LocalDate d = end.minusDays(i);
            String ds = d.format(DateTimeFormatter.ISO_LOCAL_DATE);
            running += map.getOrDefault(ds, 0L);
            Map<String, Object> m = new HashMap<>();
            m.put("date", ds);
            m.put("cumulativeBytes", running);
            out.add(m);
        }
        return out;
    }

    private String formatDate(Object o) {
        if (o instanceof Date) {
            return new SimpleDateFormat("yyyy-MM-dd").format((Date) o);
        }
        if (o instanceof LocalDate) {
            return ((LocalDate) o).format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        return o == null ? "" : o.toString();
    }
}
