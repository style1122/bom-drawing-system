package com.bom.mapper;

import com.bom.entity.ShareToken;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * 图纸分享 Token Mapper 接口
 */
public interface ShareTokenMapper extends BaseMapper<ShareToken> {

    /** 插入分享 Token */
    int insert(ShareToken shareToken);

    /** 根据 token 查询 */
    ShareToken findByToken(@Param("token") String token);

    /** 递增访问次数 */
    int incrementVisitCount(@Param("token") String token);

    /** 作废分享 */
    int invalidate(@Param("token") String token);

    /** 删除所有过期的分享记录（定时任务用） */
    int deleteExpired();
}
