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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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

    /** 单轮订阅查询最大翻页数：防御极端情况下无限翻页长期占用调度线程 */
    private static final int MAX_PULL_PAGES = 500;

    /** 轮询执行线程命名序号 */
    private static final AtomicInteger POLL_THREAD_SEQ = new AtomicInteger();

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

    /** 单轮兜底轮询的硬超时（秒）：超时则强制终止本轮，避免 ERP 接口卡死占用调度线程 */
    @Value("${erp.subscribe.poll.timeout-sec:240}")
    private int pollTimeoutSec;

    /**
     * 独立线程池执行实际拉取：与 Spring 调度线程（默认仅 1 条）隔离。
     * 即使某轮 ERP 调用卡死，调度线程也能在超时后归还并继续排下一轮，
     * 杜绝"单线程卡死 → 全部定时轮询静默失效（只有重启才同步）"的问题。
     */
    private final ExecutorService pullExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "erp-subscribe-poll-" + POLL_THREAD_SEQ.incrementAndGet());
        t.setDaemon(true);
        return t;
    });

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
        // 关键：实际拉取放到独立线程并以超时兜底，避免 ERP 接口卡死（半开连接 / 无限翻页）
        // 占住 Spring 默认的唯一调度线程，导致后续所有定时轮询静默失效（表现为"只有重启才同步"）。
        try {
            Future<Map<String, Object>> future = pullExecutor.submit(this::pullChanges);
            try {
                future.get(pollTimeoutSec, TimeUnit.SECONDS);
            } catch (TimeoutException te) {
                future.cancel(true);
                logger.error("ERP订阅轮询在 {}s 内未结束，强制终止本轮（ERP 接口可能卡死），等待下次调度",
                        pollTimeoutSec);
            } catch (Exception e) {
                logger.error("ERP订阅轮询执行异常: {}", e.getMessage());
            }
        } catch (Exception e) {
            logger.error("ERP订阅轮询提交失败: {}", e.getMessage());
        }
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
        String oldTs = readCursor(tsKey, initTimestamp);   // 上一轮水位，用于边界去重
        String oldPk = readCursor(pkKey, "");
        String timestamp = oldTs;                           // 本轮查询游标（随翻页推进）
        String pkValuesStr = readCursor(pkKey, "");

        int upserted = 0;
        int deleted = 0;
        int failed = 0;
        int pages = 0;
        boolean hasMore = true;
        boolean anyDetail = false;
        List<String> pkvalues = parsePkvalues(pkValuesStr);

        while (hasMore && pages < MAX_PULL_PAGES) {
            ErpSscrQueryResult page = erpApiClient.sscrQuery(sscrid, timestamp, pkvalues);
            pages++;

            for (ErpSscrDetail detail : page.getDetail()) {
                String dTime = detail.getLastoperatetime() == null ? "" : detail.getLastoperatetime();
                String dPk = (detail.getPkvalues() != null && !detail.getPkvalues().isEmpty())
                        ? detail.getPkvalues().get(0) : "";
                // ERP sscrquery 取 lastoperatetime >= 游标（含边界），停在游标时间戳的那条记录每轮都被重新拉回；
                // 已在上一轮处理过的(时间戳,主键)这里跳过，避免"无改动却每轮显示增删改"的重复同步。
                if (!isStrictlyAfter(dTime, dPk, oldTs, oldPk)) {
                    continue;
                }
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

            // 游标推进：从本页明细中取"最后处理的一笔"作为水位。
            // 注意：sscrquery 响应根级的 lastoperatetime/pkvalues 可能为空或回显请求时间，
            // 直接用会导致时间戳游标永远不前进、增量同步退化为每轮全量重拉。
            // 明细级字段（detail.lastoperatetime / detail.pkvalues）才是真实水位，据此推进。
            if (!page.getDetail().isEmpty()) {
                anyDetail = true;
                ErpSscrDetail pivot = null;
                for (ErpSscrDetail d : page.getDetail()) {
                    String dTime = d.getLastoperatetime() == null ? "" : d.getLastoperatetime();
                    String dPk = (d.getPkvalues() != null && !d.getPkvalues().isEmpty())
                            ? d.getPkvalues().get(0) : "";
                    if (pivot == null) {
                        pivot = d;
                        continue;
                    }
                    String pTime = pivot.getLastoperatetime() == null ? "" : pivot.getLastoperatetime();
                    String pPk = (pivot.getPkvalues() != null && !pivot.getPkvalues().isEmpty())
                            ? pivot.getPkvalues().get(0) : "";
                    // 时间更大 → 更新；时间相等且主键更大 → 更新（格式需 yyyy-MM-dd HH:mm:ss 一致，字典序即时间序）
                    boolean newer = dTime.compareTo(pTime) > 0;
                    boolean sameTimeBiggerPk = dTime.equals(pTime) && dPk.compareTo(pPk) > 0;
                    if (newer || sameTimeBiggerPk) {
                        pivot = d;
                    }
                }
                if (pivot != null) {
                    timestamp = pivot.getLastoperatetime() == null ? "" : pivot.getLastoperatetime();
                    pkvalues = pivot.getPkvalues() == null
                            ? new ArrayList<>() : new ArrayList<>(pivot.getPkvalues());
                }
            }

            hasMore = page.isHasnext();
            if (hasMore && (page.getDetail().isEmpty() || pkvalues.isEmpty())) {
                logger.warn("订阅查询返回 hasnext=true 但无分页数据，停止翻页");
                hasMore = false;
            }
        }

        if (pages >= MAX_PULL_PAGES) {
            logger.warn("订阅查询达到最大翻页上限 {}，强制结束本轮，防止调度线程被长期占用", MAX_PULL_PAGES);
            hasMore = false;
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

    /**
     * 判断 (aTime,aPk) 是否严格晚于 (bTime,bPk)：先比最后修改时间，时间相同再比主键（字典序）。
     * 用于订阅增量拉取的边界去重：ERP sscrquery 使用 lastoperatetime >= 游标（含边界），
     * 恰好停在游标时间戳的记录每轮都会被重新拉回，用本方法识别并跳过已处理过的边界记录。
     */
    private boolean isStrictlyAfter(String aTime, String aPk, String bTime, String bPk) {
        if (aTime == null) aTime = "";
        if (aPk == null) aPk = "";
        if (bTime == null) bTime = "";
        if (bPk == null) bPk = "";
        int cmp = aTime.compareTo(bTime);
        if (cmp > 0) return true;
        if (cmp < 0) return false;
        return aPk.compareTo(bPk) > 0;
    }
}
