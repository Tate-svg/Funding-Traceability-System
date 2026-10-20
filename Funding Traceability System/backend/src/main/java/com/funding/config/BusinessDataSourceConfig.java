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
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * 业务库数据源（funding_db，主数据源）
 * 扫描 com.funding.mapper 下全部业务 Mapper
 */
@Configuration
@MapperScan(basePackages = "com.funding.mapper", sqlSessionFactoryRef = "businessSqlSessionFactory")
public class BusinessDataSourceConfig {

    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource.business")
    public DataSource businessDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Primary
    @Bean
    public SqlSessionFactory businessSqlSessionFactory(
            @Qualifier("businessDataSource") DataSource dataSource,
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

    @Primary
    @Bean
    public SqlSessionTemplate businessSqlSessionTemplate(
            @Qualifier("businessSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
