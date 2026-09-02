package com.bom.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 插件配置
 *
 * <p>注册分页拦截器（SQL Server 2019 使用 DbType.SQL_SERVER，基于 OFFSET/FETCH）。
 * XML Mapper 中已有的物理分页语句不受影响，此拦截器主要为 BaseMapper 的
 * selectPage / 使用 QueryWrapper 的分页提供服务。</p>
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.SQL_SERVER));
        return interceptor;
    }
}
