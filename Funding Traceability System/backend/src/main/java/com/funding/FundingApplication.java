package com.funding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 助学帮扶管理系统 启动类
 * Mapper 扫描见双数据源配置：BusinessDataSourceConfig(com.funding.mapper) /
 * PrivacyDataSourceConfig(com.funding.privacymapper)
 */
@SpringBootApplication
public class FundingApplication {

    public static void main(String[] args) {
        SpringApplication.run(FundingApplication.class, args);
    }
}
