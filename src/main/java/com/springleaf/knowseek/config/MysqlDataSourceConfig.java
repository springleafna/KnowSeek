package com.springleaf.knowseek.config;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@MapperScan(
        basePackages = "com.springleaf.knowseek.mapper.mysql",
        sqlSessionTemplateRef = "mysqlSqlSessionTemplate"
)
public class MysqlDataSourceConfig {

    @Bean("mysqlDataSource")
    @Primary
    public DataSource mysqlDataSource(
            @Value("${spring.datasource.mysql.driver-class-name}") String driverClassName,
            @Value("${spring.datasource.mysql.url}") String url,
            @Value("${spring.datasource.mysql.username}") String username,
            @Value("${spring.datasource.mysql.password}") String password,
            @Value("${spring.datasource.mysql.hikari.maximum-pool-size:10}") int maximumPoolSize,
            @Value("${spring.datasource.mysql.hikari.minimum-idle:5}") int minimumIdle,
            @Value("${spring.datasource.mysql.hikari.idle-timeout:300000}") long idleTimeout,
            @Value("${spring.datasource.mysql.hikari.connection-timeout:30000}") long connectionTimeout,
            @Value("${spring.datasource.mysql.hikari.max-lifetime:600000}") long maxLifetime,
            @Value("${spring.datasource.mysql.hikari.validation-timeout:5000}") long validationTimeout,
            @Value("${spring.datasource.mysql.hikari.pool-name}") String poolName) {

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(driverClassName);
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        dataSource.setMaximumPoolSize(maximumPoolSize);
        dataSource.setMinimumIdle(minimumIdle);
        dataSource.setIdleTimeout(idleTimeout);
        dataSource.setConnectionTimeout(connectionTimeout);
        dataSource.setMaxLifetime(maxLifetime);
        dataSource.setPoolName(poolName);
        dataSource.setValidationTimeout(validationTimeout);

        return dataSource;
    }

    @Bean("mysqlJdbcTemplate")
    public JdbcTemplate mysqlJdbcTemplate(@Qualifier("mysqlDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean("mysqlSqlSessionFactory")
    public SqlSessionFactory mysqlSqlSessionFactory(
            @Qualifier("mysqlDataSource") DataSource mysqlDataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(mysqlDataSource);
        factoryBean.setMapperLocations(
                new PathMatchingResourcePatternResolver()
                        .getResources("classpath:/mapper/mysql/*.xml")
        );
        return factoryBean.getObject();
    }

    @Bean("mysqlSqlSessionTemplate")
    public SqlSessionTemplate mysqlSqlSessionTemplate(
            @Qualifier("mysqlSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean("mysqlTransactionManager")
    @Primary
    public PlatformTransactionManager mysqlTransactionManager(@Qualifier("mysqlDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}