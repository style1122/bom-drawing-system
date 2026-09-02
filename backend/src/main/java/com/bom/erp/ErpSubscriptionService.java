package com.bom.erp;

import com.bom.entity.Material;
import com.bom.mapper.DrawingMapper;
import com.bom.mapper.ErpSyncCursorMapper;
import com.bom.mapper.MaterialMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 正航 T9 ERP 订阅同步服务。
 * <p>
 * 流程：ERP 物料异动 → 通知本系统回调接口 → 本系统调用 /esb/erp/sscrquery.do
 * 按最后修改时间增量拉取异动数据 → action=0 新增/修改 upsert，action=2 删除。
 * 增量游标（最后修改时间 + 最后主键）持久化到 erp_sync_cursor 表，服务重启后继续。
 */
@Service
public class ErpSubscriptionService {

    private static final Logger logger = LoggerFactory.getLogger(ErpSubscriptionService.class);

    @Value("${erp.subscribe.enabled:true}")
    private boolean subscribeEnabled;

    @Value("${erp.subscribe.sscrid:MA01}")
    private String sscrid;

    /** 无游标时的初始查询时间戳 */
    @Value("${erp.subscribe.init-timestamp:2018-01-01 00:00:00}")
    private String initTimestamp;

    /** 兜底轮询开关：实时通知正常时可关闭；默认开启 */
    @Value("${erp.subscribe.poll.enabled:true}")
    private boolean pollEnabled;

    @Autowired
    private ErpApiClient erpApiClient;

    @Autowired
    private ErpMaterialSyncService erpMaterialSyncService;

    @Autowired
    private MaterialMapper materialMapper;

    @Autowired
    private DrawingMapper drawingMapper;

    @Autowired
    private ErpSyncCursorMapper erpSyncCursorMapper;

    /** 防止重复拉取 */
    private final AtomicBoolean pulling = new AtomicBoolean(false);

    private volatile Map<String, Object> lastResult;

    public boolean isEnabled() {
        return subscribeEnabled;
    }

    public String getSscrid() {
        return sscrid;
    }

    public Map<String, Object> getLastResult() {
        return lastResult;
    }

    /**
     * 定时轮询订阅查询接口：即使 ERP 回调通知未配置/丢失，也能在间隔内同步物料异动。
     */
    @Scheduled(fixedDelayString = "${erp.subscribe.poll.interval-ms:300000}")
    public void scheduledPull() {
        if (!subscribeEnabled || !pollEnabled) {
            return;
        }
        pullChanges();
    }

    /**
     * 处理 ERP 订阅通知：直接用通知报文中的物料数据同步（实时，不依赖 sscrquery 的分钟级延迟）。
     * action：0=新增/更新，2=删除（或 DeleteTime 非空）。
     */
    public Map<String, Object> processNotification(Map<String, Object> body) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (!subscribeEnabled) {
            result.put("success", false);
            result.put("message", "订阅同步未启用");
            return result;
        }
        int processed = 0;
        int deleted = 0;
        int failed = 0;

        int action = body.get("action") instanceof Number ? ((Number) body.get("action")).intValue() : -1;
        Object dataObj = body.get("data");
        if (dataObj instanceof List) {
            for (Object itemObj : (List<?>) dataObj) {
                if (!(itemObj instanceof Map)) {
                    continue;
                }
                Object groupObj = ((Map<?, ?>) itemObj).get("comMaterialGroup");
                if (!(groupObj instanceof List)) {
                    continue;
                }
                for (Object matObj : (List<?>) groupObj) {
                    if (!(matObj instanceof Map)) {
                        continue;
                    }
                    Map<?, ?> mat = (Map<?, ?>) matObj;
                    String code = str(mat.get("MaterialId"));
                    String deleteTime = str(mat.get("DeleteTime"));
                    boolean isDelete = action == 2 || (deleteTime != null && !deleteTime.isEmpty());
                    try {
                        if (isDelete) {
                            deleted += handleDelete(code);
                        } else {
                            upsertFromNotification(mat);
                            processed++;
                        }
                    } catch (Exception e) {
                        failed++;
                        logger.error("订阅通知同步物料失败: {} - {}", code, e.getMessage(), e);
                    }
                }
            }
        }

        result.put("success", true);
        result.put("processed", processed);
        result.put("deleted", deleted);
        result.put("failed", failed);
        result.put("message", String.format("订阅通知直连同步完成：处理 %d 条，删除 %d 条，失败 %d 条",
                processed, deleted, failed));
        logger.info(result.get("message").toString());
        return result;
    }

    /**
     * 用通知报文中的物料字段 upsert 本地记录。
     * 注意：通知报文仅含部分字段（编码/名称/规格/图纸标记），
     * 类型/单位等其余字段由定时订阅查询在索引追上后补齐。
     */
    private void upsertFromNotification(Map<?, ?> mat) {
        String code = str(mat.get("MaterialId"));
        if (code == null || code.isEmpty()) {
            return;
        }
        ErpMaterial m = new ErpMaterial();
        m.setMaterialId(code);
        m.setMaterialName(str(mat.get("MaterialName")));
        m.setMaterialSpec(str(mat.get("MaterialSpec")));
        Object haveDrawing = mat.get("CU_HaveDrawing");
        if (haveDrawing instanceof Boolean) {
            m.setCuHaveDrawing((Boolean) haveDrawing);
        } else if (haveDrawing instanceof Number) {
            m.setCuHaveDrawing(((Number) haveDrawing).intValue() == 1);
        }
        erpMaterialSyncService.upsertMaterial(m);
        if (m.getCuHaveDrawing() != null) {
            materialMapper.updateErpHaveDrawing(code, m.getCuHaveDrawing() ? 1 : 0);
        }
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    /**
     * 手动触发一次订阅增量拉取（供测试/接口调用）。
     */
    public Map<String, Object> pullChanges() {
        if (!pulling.compareAndSet(false, true)) {
            Map<String, Object> busy = new LinkedHashMap<>();
            busy.put("success", false);
            busy.put("message", "订阅拉取正在进行中");
            return busy;
        }
        try {
            lastResult = pullChangesInternal();
            return lastResult;
        } catch (Exception e) {
            logger.error("ERP 订阅增量拉取失败", e);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("message", "订阅拉取失败: " + e.getMessage());
            lastResult = result;
            return result;
        } finally {
            pulling.set(false);
        }
    }

    private Map<String, Object> pullChangesInternal() {
        String tsKey = cursorKey("timestamp");
        String pkKey = cursorKey("pkvalues");
        String timestamp = readCursor(tsKey, initTimestamp);
        String pkValuesStr = readCursor(pkKey, "");

        int upserted = 0;
        int deleted = 0;
        int failed = 0;
        int pages = 0;
        boolean hasMore = true;
        boolean anyDetail = false;
        List<String> pkvalues = parsePkvalues(pkValuesStr);

        while (hasMore) {
            ErpSscrQueryResult page = erpApiClient.sscrQuery(sscrid, timestamp, pkvalues);
            pages++;

            for (ErpSscrDetail detail : page.getDetail()) {
                String code = detail.getPkvalues() == null || detail.getPkvalues().isEmpty()
                        ? null : detail.getPkvalues().get(0);
                try {
                    if (detail.getAction() == 2) {
                        deleted += handleDelete(code);
                    } else if (detail.getData() != null) {
                        erpMaterialSyncService.upsertMaterial(detail.getData());
                        // 同步 ERP 返回的图纸标记到本地记录，保持两侧一致
                        if (detail.getData().getCuHaveDrawing() != null) {
                            materialMapper.updateErpHaveDrawing(detail.getData().getMaterialId(),
                                    detail.getData().getCuHaveDrawing() ? 1 : 0);
                        }
                        upserted++;
                    }
                } catch (Exception e) {
                    failed++;
                    logger.error("订阅同步物料失败: {} - {}", code, e.getMessage(), e);
                }
            }

            // 游标推进：仅在有异动数据时取本次返回的最后一笔最后修改时间与主键
            if (!page.getDetail().isEmpty()) {
                anyDetail = true;
                timestamp = page.getLastoperatetime();
                pkvalues = page.getPkvalues();
            }

            hasMore = page.isHasnext();
            if (hasMore && (page.getDetail().isEmpty() || pkvalues.isEmpty())) {
                logger.warn("订阅查询返回 hasnext=true 但无分页数据，停止翻页");
                hasMore = false;
            }
        }

        // 无任何异动时不写游标，减少空轮询的数据库开销
        if (anyDetail) {
            if (timestamp != null && !timestamp.isEmpty()) {
                erpSyncCursorMapper.upsert(tsKey, timestamp);
            }
            erpSyncCursorMapper.upsert(pkKey, joinPkvalues(pkvalues));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("pages", pages);
        result.put("upserted", upserted);
        result.put("deleted", deleted);
        result.put("failed", failed);
        result.put("cursorTimestamp", timestamp);
        result.put("message", String.format("ERP订阅同步完成：新增/修改 %d 条，删除 %d 条，失败 %d 条，共 %d 页",
                upserted, deleted, failed, pages));
        logger.info(result.get("message").toString());
        return result;
    }

    /**
     * 处理删除（action=2）：删除本地物料及其图纸记录（磁盘文件保留，可另行清理）。
     */
    private int handleDelete(String code) {
        if (code == null || code.isEmpty()) {
            return 0;
        }
        Material material = materialMapper.findByCode(code);
        if (material == null) {
            return 0;
        }
        drawingMapper.deleteByMaterialId(material.getId());
        materialMapper.deleteByCode(code);
        logger.info("ERP订阅：删除物料 {} ({})", code, material.getMaterialName());
        return 1;
    }

    private String cursorKey(String suffix) {
        return "material." + sscrid + "." + suffix;
    }

    private String readCursor(String key, String defaultValue) {
        com.bom.entity.ErpSyncCursor cursor = erpSyncCursorMapper.findByKey(key);
        if (cursor == null || cursor.getCursorValue() == null || cursor.getCursorValue().isEmpty()) {
            return defaultValue;
        }
        return cursor.getCursorValue();
    }

    private List<String> parsePkvalues(String value) {
        if (value == null || value.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(value.split(",")));
    }

    private String joinPkvalues(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return String.join(",", values);
    }
}
