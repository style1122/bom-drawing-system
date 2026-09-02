package com.bom;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * BOM 图纸管理系统 —— Spring Boot 启动类
 *
 * <p>扫描根包 com.bom 下所有 @Component/@Controller/@Service/@Repository；
 * @MapperScan 扫描 MyBatis-Plus Mapper 接口（com.bom.mapper）；
 * @EnableScheduling 启用 ERP 物料定时同步等 @Scheduled 任务。</p>
 */
@SpringBootApplication(scanBasePackages = "com.bom")
@MapperScan("com.bom.mapper")
@EnableScheduling
public class BomDrawingSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(BomDrawingSystemApplication.class, args);
    }
}
