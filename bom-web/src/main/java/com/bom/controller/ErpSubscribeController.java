package com.bom.controller;

import com.bom.erp.ErpSubscriptionService;
import com.bom.service.AuditLogService;
import com.bom.util.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 正航 T9 ERP 订阅回调接口。
 * <p>
 * ERP 物料数据异动时回调 POST /api/erp/subscribe/material，
 * 本接口校验订阅号后立即应答，并异步触发订阅增量拉取。
 */
@RestController
@RequestMapping("/api/erp/subscribe")
public class ErpSubscribeController {

    private static final Logger logger = LoggerFactory.getLogger(ErpSubscribeController.class);

    @Autowired
    private ErpSubscriptionService subscriptionService;

    @Autowired
    private AuditLogService auditLogService;

    @Value("${erp.subscribe.sscrid:MA01}")
    private String sscrid;

    /** 可选回调密钥：配置后 ERP 回调需在请求头 X-Subscribe-Secret 携带 */
    @Value("${erp.subscribe.secret:}")
    private String secret;

    /**
     * ERP 订阅通知回调（物料基础数据）。
     */
    @PostMapping("/material")
    public Result notify(@RequestBody(required = false) Map<String, Object> body,
                         @RequestHeader(value = "X-Subscribe-Secret", required = false) String headerSecret,
                         HttpServletRequest request) {
        logger.info("收到ERP订阅通知报文: {}", body);

        // 可选密钥校验
        if (!secret.isEmpty() && !secret.equals(headerSecret)) {
            logger.warn("ERP订阅通知密钥校验失败");
            return Result.error(401, "订阅密钥校验失败");
        }

        // 订阅号校验：大小写不敏感查找；ERP 报文可能不带订阅号字段，此时按端点本身放行
        Object receivedSscrid = findBodyValue(body, "sscrid");
        if (receivedSscrid != null && !sscrid.equalsIgnoreCase(String.valueOf(receivedSscrid))) {
            logger.warn("ERP订阅通知订阅号不匹配: {}", receivedSscrid);
            return Result.error(400, "订阅号不匹配");
        }
        if (receivedSscrid == null) {
            logger.warn("ERP订阅通知未包含订阅号字段，按订阅端点放行（sscrid={}）", sscrid);
        }

        Object funcid = findBodyValue(body, "funcid");
        Object lastOperateTime = findBodyValue(body, "lastoperatetime");
        logger.info("处理ERP订阅通知: funcid={}, sscrid={}, lastoperatetime={}",
                funcid, receivedSscrid, lastOperateTime);
        auditLogService.log(0L, "ERP订阅通知", "MATERIAL", null,
                "funcid=" + funcid + ", lastoperatetime=" + lastOperateTime, request.getRemoteAddr());

        // 用通知报文直连同步（实时），完成后应答
        Map<String, Object> syncResult = subscriptionService.processNotification(body);
        return Result.success("ok", syncResult);
    }

    /**
     * 大小写不敏感地从通知报文中取字段值。
     */
    private Object findBodyValue(Map<String, Object> body, String key) {
        if (body == null) {
            return null;
        }
        for (Map.Entry<String, Object> entry : body.entrySet()) {
            if (key.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 查询订阅状态与最近一次拉取结果。
     */
    @GetMapping("/status")
    public Result status() {
        Map<String, Object> data = new HashMap<>();
        data.put("enabled", subscriptionService.isEnabled());
        data.put("sscrid", subscriptionService.getSscrid());
        data.put("lastResult", subscriptionService.getLastResult());
        return Result.success(data);
    }

    /**
     * 手动触发一次订阅增量拉取（联调/测试用，GET/POST 均可）。
     */
    @RequestMapping(value = "/pull", method = {RequestMethod.GET, RequestMethod.POST})
    public Result pull() {
        return Result.success(subscriptionService.pullChanges());
    }
}
