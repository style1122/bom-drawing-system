package com.bom.service;

import com.bom.entity.*;
import com.bom.exception.BusinessException;
import com.bom.mapper.ShareTokenMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 图纸分享业务服务
 */
@Service
public class ShareService {

    private static final Logger logger = LoggerFactory.getLogger(ShareService.class);

    @Autowired
    private ShareTokenMapper shareTokenMapper;

    @Autowired
    private MaterialService materialService;

    @Autowired
    private DrawingService drawingService;

    /** 分享链接默认有效期（天） */
    private static final int DEFAULT_EXPIRE_DAYS = 7;

    /**
     * 创建分享 Token
     * @param materialId 物料ID
     * @param userId     创建人ID
     * @return 分享 token 字符串
     */
    public String createShareToken(Long materialId, Long userId) {
        // 验证物料存在
        Material material = materialService.getById(materialId);
        if (material == null) {
            throw new BusinessException("物料不存在");
        }

        // 生成唯一 token（UUID 去掉横线）
        String token = UUID.randomUUID().toString().replace("-", "");

        // 过期时间 = 当前时间 + 7 天
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, DEFAULT_EXPIRE_DAYS);
        Date expireAt = cal.getTime();

        ShareToken st = new ShareToken();
        st.setToken(token);
        st.setMaterialId(materialId);
        st.setCreatedBy(userId);
        st.setExpireAt(expireAt);
        st.setVisitCount(0);
        st.setIsValid(1);

        shareTokenMapper.insert(st);
        logger.info("创建分享 Token: token={}, materialId={}, userId={}, expireAt={}",
                token, materialId, userId, expireAt);

        return token;
    }

    /**
     * 验证 Token 是否有效
     * @return ShareToken 实体
     */
    public ShareToken validateToken(String token) {
        ShareToken st = shareTokenMapper.findByToken(token);
        if (st == null) {
            throw new BusinessException(404, "分享链接不存在");
        }
        if (st.getIsValid() != 1) {
            throw new BusinessException(410, "分享链接已失效");
        }
        if (new Date().after(st.getExpireAt())) {
            throw new BusinessException(410, "分享链接已过期");
        }

        // 更新访问计数
        shareTokenMapper.incrementVisitCount(token);
        return st;
    }

    /**
     * 获取分享页完整数据（物料信息 + 图纸列表）
     */
    public Map<String, Object> getShareData(String token) {
        ShareToken st = validateToken(token);

        Material material = materialService.getById(st.getMaterialId());
        if (material == null) {
            throw new BusinessException("物料不存在");
        }

        List<Drawing> drawings = drawingService.getByMaterialId(st.getMaterialId());

        Map<String, Object> data = new HashMap<>();
        data.put("material", material);
        data.put("drawings", drawings != null ? drawings : Collections.emptyList());
        return data;
    }

    /**
     * 获取分享图纸信息（带权限验证）
     */
    public Drawing getShareDrawing(Long drawingId, String token) {
        ShareToken st = validateToken(token);

        Drawing drawing = drawingService.getById(drawingId);
        if (drawing == null) {
            throw new BusinessException("图纸不存在");
        }

        // 验证图纸是否属于该分享的物料
        if (!st.getMaterialId().equals(drawing.getMaterialId())) {
            throw new BusinessException("无权访问该图纸");
        }

        return drawing;
    }

    /**
     * 获取图纸文件存储路径（带权限验证）
     */
    public String getShareDrawingPath(Long drawingId, String token) {
        getShareDrawing(drawingId, token); // 验证权限
        return drawingService.download(drawingId);
    }

    /**
     * 作废分享链接
     */
    public void invalidate(String token) {
        int affected = shareTokenMapper.invalidate(token);
        if (affected == 0) {
            throw new BusinessException("分享链接不存在");
        }
        logger.info("作废分享 Token: token={}", token);
    }
}
