package com.funding.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import org.apache.ibatis.logging.stdout.StdOutImpl;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * 隐私库数据源（funding_privacy，隔离数据源）
 * 仅扫描 com.funding.privacymapper；禁止跨库 JOIN，业务侧通过 virtual_id 手工映射
 */
@Configuration
@MapperScan(basePackages = "com.funding.privacymapper", sqlSessionFactoryRef = "privacySqlSessionFactory")
public class PrivacyDataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.privacy")
    public DataSource privacyDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    public SqlSessionFactory privacySqlSessionFactory(
            @Qualifier("privacyDataSource") DataSource dataSource,
            MybatisPlusInterceptor interceptor) throws Exception {
        com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean bean =
                new com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        com.baomidou.mybatisplus.core.MybatisConfiguration cfg =
                new com.baomidou.mybatisplus.core.MybatisConfiguration();
        cfg.setMapUnderscoreToCamelCase(true);
        cfg.setLogImpl(StdOutImpl.class);
        bean.setConfiguration(cfg);
        bean.setPlugins(interceptor);
        return bean.getObject();
    }

    @Bean
    public SqlSessionTemplate privacySqlSessionTemplate(
            @Qualifier("privacySqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
