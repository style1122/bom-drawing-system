package com.bom.erp;

import com.bom.exception.BusinessException;
import com.bom.util.MD5Util;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 正航 T9 ERP ESB 客户端。
 * <p>
 * 认证：POST /esb/api/auth.do，appid+appsecret+time 做 MD5 签名，返回 token。
 * 业务：POST /esb/erp/getlist.do，token+appsecret+time 做 MD5 签名，progid=Mat01 分页拉取物料。
 */
@Component
public class ErpApiClient {

    private static final Logger logger = LoggerFactory.getLogger(ErpApiClient.class);

    @Value("${erp.base-url:http://10.1.1.15:860}")
    private String baseUrl;

    @Value("${erp.appid:}")
    private String appid;

    @Value("${erp.appsecret:}")
    private String appsecret;

    @Value("${erp.progid:Mat01}")
    private String progid;

    @Value("${erp.language:zh-CHS}")
    private String language;

    /** millis6 = UTC毫秒 + 6位随机数；datetime14 = yyyyMMddHHmmss */
    @Value("${erp.time-format:millis6}")
    private String timeFormat;

    @Value("${erp.connect-timeout:10000}")
    private int connectTimeout;

    @Value("${erp.read-timeout:120000}")
    private int readTimeout;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private volatile RestTemplate restTemplate;

    /** 内存 token 缓存 */
    private volatile String token;
    private volatile long tokenExpireAt; // epoch millis

    private static class TokenResult {
        final String token;
        final long timeoutSeconds;
        TokenResult(String token, long timeoutSeconds) {
            this.token = token;
            this.timeoutSeconds = timeoutSeconds;
        }
    }

    /**
     * 获取有效 token，过期或缺失时自动重新认证。
     */
    public synchronized String getToken() {
        if (token != null && System.currentTimeMillis() < tokenExpireAt - 60_000L) {
            return token;
        }
        TokenResult tr = auth();
        this.token = tr.token;
        this.tokenExpireAt = System.currentTimeMillis() + tr.timeoutSeconds * 1000L;
        logger.info("ERP 认证成功，token 有效期 {} 秒", tr.timeoutSeconds);
        return this.token;
    }

    /**
     * 测试 ERP 连接与认证凭据。
     *
     * @return 认证信息（token、有效期等）
     */
    public Map<String, Object> testConnection() {
        TokenResult tr = auth();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "ERP 认证成功");
        result.put("timeout", tr.timeoutSeconds);
        result.put("serverTime", new Date());
        return result;
    }

    /**
     * 返回当前生效的 ERP 连接配置（不含密钥），用于运行期排查。
     */
    public Map<String, Object> getConfigInfo() {
        Map<String, Object> result = new HashMap<>();
        result.put("baseUrl", baseUrl);
        result.put("progid", progid);
        result.put("timeFormat", timeFormat);
        result.put("connectTimeout", connectTimeout);
        result.put("readTimeout", readTimeout);
        result.put("appidConfigured", appid != null && !appid.isEmpty());
        result.put("appsecretConfigured", appsecret != null && !appsecret.isEmpty());
        return result;
    }

    /**
     * 调用认证接口获取 token。
     */
    public TokenResult auth() {
        validateConfig();
        // 注意：该 ESB 的 CapCRL 解析器对认证接口要求字段固定顺序：
        // appid -> sign -> language -> time（文档示例顺序会被解析失败）
        Map<String, Object> body = new LinkedHashMap<>();
        String time = buildTime();
        body.put("appid", appid);
        body.put("sign", MD5Util.md5(appid + appsecret + time));
        if (language != null && !language.isEmpty()) {
            body.put("language", language);
        }
        body.put("time", time);

        JsonNode root = postJson(baseUrl + "/esb/api/auth.do", body);
        int status = root.path("status").asInt(-1);
        if (status != 1) {
            String error = serverError(root);
            throw new BusinessException("ERP认证失败: " + error);
        }
        String token = root.path("token").asText();
        long timeout = root.path("timeout").asLong(7200);
        if (token.isEmpty()) {
            throw new BusinessException("ERP认证失败: 响应中缺少 token");
        }
        return new TokenResult(token, timeout);
    }

    /**
     * 分页拉取物料清单（getlist）。
     *
     * @param condition     符合 SQL 语法的 where 条件，可为 null/空串
     * @param lastpkvalues  上一页返回的最后一笔主键，第一页传 null/空串
     */
    public ErpGetListResult getList(String condition, String lastpkvalues) {
        return getList(getToken(), condition, lastpkvalues);
    }

    /**
     * 分页拉取物料清单（getlist），使用指定 token。
     */
    public ErpGetListResult getList(String token, String condition, String lastpkvalues) {
        Map<String, Object> body = new LinkedHashMap<>();
        String time = buildTime();
        body.put("token", token);
        body.put("time", time);
        body.put("sign", MD5Util.md5(token + appsecret + time));
        body.put("progid", progid);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("condition", condition == null ? "" : condition);
        data.put("lastpkvalues", lastpkvalues == null ? "" : lastpkvalues);
        body.put("data", data);

        JsonNode root = postJson(baseUrl + "/esb/erp/getlist.do", body);
        int status = root.path("status").asInt(-1);
        if (status != 1) {
            String error = serverError(root);
            String errorcode = root.path("errorcode").asText("");
            throw new BusinessException("ERP物料查询失败(" + errorcode + "): " + error);
        }

        ErpGetListResult result = new ErpGetListResult();
        result.setStatus(1);
        result.setLastpkvalues(root.path("lastpkvalues").asText(""));
        result.setHasnext(root.path("hasnext").asBoolean(false));

        JsonNode dataNode = root.path("data");
        JsonNode materialGroup = dataNode.path("MaterialGroup");
        if (materialGroup.isArray()) {
            for (JsonNode node : materialGroup) {
                result.getMaterials().add(parseMaterialGroup(node));
            }
        }
        return result;
    }

    /**
     * 更新物料基础数据的“是否存在图纸”标记（CU_HaveDrawing：1=是，0=否）。
     * 接口：POST /esb/erp/update.do，progid=Mat01。
     *
     * @param materialCode 物料代码（ERP MaterialId）
     * @param haveDrawing  是否存在图纸
     */
    public void updateMaterialHaveDrawing(String materialCode, boolean haveDrawing) {
        validateConfig();
        String token = getToken();
        Map<String, Object> body = new LinkedHashMap<>();
        String time = buildTime();
        body.put("token", token);
        body.put("time", time);
        body.put("sign", MD5Util.md5(token + appsecret + time));
        body.put("progid", progid);

        Map<String, Object> materialGroup = new LinkedHashMap<>();
        materialGroup.put("MaterialId", materialCode);
        materialGroup.put("CU_HaveDrawing", haveDrawing ? 1 : 0);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("MaterialGroup", materialGroup);
        body.put("data", data);

        JsonNode root = postJson(baseUrl + "/esb/erp/update.do", body);
        int status = root.path("status").asInt(-1);
        if (status != 1) {
            throw new BusinessException("ERP更新失败: " + serverError(root));
        }
        logger.info("ERP物料图纸标记更新成功: {} -> {}", materialCode, haveDrawing ? 1 : 0);
    }

    /**
     * 订阅查询：按最后修改时间增量拉取物料异动数据。
     * 接口：POST /esb/erp/sscrquery.do，订阅号 sscrid。
     *
     * @param sscrid     订阅号（如 MA01）
     * @param timestamp  上次查询最后一笔的最后修改时间（第一页传 null 或初始时间）
     * @param pkvalues   上次查询最后一笔的主键（第一页传 null）
     */
    public ErpSscrQueryResult sscrQuery(String sscrid, String timestamp, List<String> pkvalues) {
        validateConfig();
        String token = getToken();
        Map<String, Object> body = new LinkedHashMap<>();
        String time = buildTime();
        body.put("token", token);
        body.put("time", time);
        body.put("sign", MD5Util.md5(token + appsecret + time));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sscrid", sscrid);
        data.put("timestamp", timestamp == null ? "" : timestamp);
        // 第一页或无可分页主键时必须传 null，否则 ERP 会拼出空值条件 (MaterialId>) 导致语法错误
        String lastPk = (pkvalues == null || pkvalues.isEmpty()) ? null : pkvalues.get(0);
        data.put("pkvalues", lastPk);
        body.put("data", data);

        JsonNode root = postJson(baseUrl + "/esb/erp/sscrquery.do", body);
        int status = root.path("status").asInt(-1);
        if (status != 1) {
            throw new BusinessException("ERP订阅查询失败: " + serverError(root));
        }

        ErpSscrQueryResult result = new ErpSscrQueryResult();
        result.setStatus(1);
        result.setFuncid(root.path("funcid").asText(""));
        result.setHasnext(root.path("hasnext").asBoolean(false));
        result.setLastoperatetime(root.path("lastoperatetime").asText(""));
        result.setPkvalues(jsonArrayToStringList(root.path("pkvalues")));

        JsonNode detailNode = root.path("detail");
        if (detailNode.isArray()) {
            for (JsonNode node : detailNode) {
                ErpSscrDetail detail = new ErpSscrDetail();
                detail.setPkvalues(jsonArrayToStringList(node.path("pkvalues")));
                detail.setLastoperatetime(node.path("lastoperatetime").asText(""));
                detail.setAction(node.path("action").asInt(0));
                JsonNode dataNode = node.path("data").path("MaterialGroup");
                if (dataNode.isObject()) {
                    detail.setData(parseMaterialGroup(dataNode));
                }
                result.getDetail().add(detail);
            }
        }
        return result;
    }

    private List<String> jsonArrayToStringList(JsonNode node) {
        List<String> list = new java.util.ArrayList<>();
        if (node.isArray()) {
            for (JsonNode item : node) {
                if (!item.isNull()) {
                    list.add(item.asText());
                }
            }
        }
        return list;
    }

    private ErpMaterial parseMaterialGroup(JsonNode node) {
        ErpMaterial m = new ErpMaterial();
        m.setMaterialId(text(node, "MaterialId"));
        m.setMaterialName(text(node, "MaterialName"));
        m.setMaterialTypeId(text(node, "MaterialTypeId"));
        m.setMaterialSpec(text(node, "MaterialSpec"));
        m.setMaterialCategoryId(text(node, "MaterialCategoryId"));
        m.setUnitId(text(node, "UnitId"));
        m.setValidityFromDate(text(node, "ValidityFromDate"));
        m.setValidityToDate(text(node, "ValidityToDate"));
        JsonNode haveDrawing = node.get("CU_HaveDrawing");
        m.setCuHaveDrawing(haveDrawing == null || haveDrawing.isNull() ? null : haveDrawing.asBoolean());
        return m;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText();
    }

    /**
     * 提取服务端错误信息：兼容 errorMsg / error 字段，以及 status 为 "fails" 的字符串形式。
     */
    private String serverError(JsonNode root) {
        String errorMsg = root.path("errorMsg").asText("");
        if (!errorMsg.isEmpty()) {
            return errorMsg;
        }
        String error = root.path("error").asText("");
        if (!error.isEmpty()) {
            return error;
        }
        String status = root.path("status").asText("");
        return status.isEmpty() ? "未知错误" : "状态=" + status;
    }

    /**
     * POST JSON 并解析响应。
     */
    private JsonNode postJson(String url, Map<String, Object> body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
            String resp = getRestTemplate().postForObject(url, entity, String.class);
            if (resp == null || resp.trim().isEmpty()) {
                throw new BusinessException("ERP响应为空: " + url);
            }
            return objectMapper.readTree(resp);
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientException e) {
            logger.error("ERP请求失败: {} - {}", url, e.getMessage());
            throw new BusinessException("ERP请求失败(网络或服务不可达): " + e.getMessage());
        } catch (Exception e) {
            logger.error("ERP响应解析失败: {} - {}", url, e.getMessage(), e);
            throw new BusinessException("ERP响应解析失败: " + e.getMessage());
        }
    }

    private RestTemplate getRestTemplate() {
        if (restTemplate == null) {
            synchronized (this) {
                if (restTemplate == null) {
                    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                    factory.setConnectTimeout(connectTimeout);
                    factory.setReadTimeout(readTimeout);
                    restTemplate = new RestTemplate(factory);
                }
            }
        }
        return restTemplate;
    }

    /**
     * 生成请求 time 参数。
     */
    private String buildTime() {
        if ("datetime14".equalsIgnoreCase(timeFormat)) {
            return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        }
        // 默认：UTC 毫秒时间戳 + 6 位随机数
        long millis = System.currentTimeMillis();
        int random = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return String.valueOf(millis) + random;
    }

    /**
     * 校验 ERP 配置是否完整，避免因配置缺失导致难以定位的认证错误。
     */
    private void validateConfig() {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new BusinessException("ERP配置缺失: erp.base-url 未配置");
        }
        if (appid == null || appid.trim().isEmpty()) {
            throw new BusinessException("ERP配置缺失: erp.appid 未配置");
        }
        if (appsecret == null || appsecret.trim().isEmpty()) {
            throw new BusinessException("ERP配置缺失: erp.appsecret 未配置");
        }
    }
}
