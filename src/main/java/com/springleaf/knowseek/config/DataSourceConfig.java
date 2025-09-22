package com.springleaf.knowseek.config;

import com.zaxxer.hikari.HikariDataSource;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
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
import java.util.Objects;

/**
 * 多数据源配置
 */
@Configuration
public class DataSourceConfig {

    @Bean("mysqlDataSource")
    @Primary
    public DataSource mysqlDataSource(
            @Value("${spring.datasource.mysql.driver-class-name}") String driverClassName,
            @Value("${spring.datasource.mysql.url}") String url,
            @Value("${spring.datasource.mysql.username}") String username,
            @Value("${spring.datasource.mysql.password}") String password) {

        // 创建HikariCP连接池
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(driverClassName);
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        return dataSource;
    }

    @Bean("mysqlJdbcTemplate")
    public JdbcTemplate mysqlJdbcTemplate(@Qualifier("mysqlDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean("mysqlSqlSessionFactory")
    public SqlSessionFactoryBean mysqlSqlSessionFactory(@Qualifier("mysqlDataSource") DataSource mysqlDataSource) throws Exception {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(mysqlDataSource);

        // 设置Mapper XML文件位置
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        sqlSessionFactoryBean.setMapperLocations(resolver.getResources("classpath:/mapper/mysql/*.xml"));

        return sqlSessionFactoryBean;
    }

    @Bean("mysqlSqlSessionTemplate")
    public SqlSessionTemplate sqlSessionTemplate(@Qualifier("mysqlSqlSessionFactory") SqlSessionFactoryBean sqlSessionFactory) throws Exception {
        return new SqlSessionTemplate(Objects.requireNonNull(sqlSessionFactory.getObject()));
    }

    @Bean("mysqlTransactionManager")
    @Primary
    public PlatformTransactionManager mysqlTransactionManager(@Qualifier("mysqlDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean("pgVectorDataSource")
    public DataSource pgVectorDataSource(
            @Value("${spring.datasource.pgvector.driver-class-name}") String driverClassName,
            @Value("${spring.datasource.pgvector.url}") String url,
            @Value("${spring.datasource.pgvector.username}") String username,
            @Value("${spring.datasource.pgvector.password}") String password) {

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(driverClassName);
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        // 向量库特殊配置
        dataSource.setInitializationFailTimeout(1);        // 1ms快速失败
        dataSource.setConnectionTestQuery("SELECT 1");      // 连接测试查询
        dataSource.setAutoCommit(true);                     // 自动提交事务
        dataSource.setPoolName("PgVectorHikariPool");       // 连接池名称

        dataSource.setMaximumPoolSize(20);
        dataSource.setMinimumIdle(5);
        dataSource.setConnectionTimeout(30000);
        dataSource.setIdleTimeout(600000);
        dataSource.setMaxLifetime(1200000);

        return dataSource;
    }

    @Bean("pgVectorJdbcTemplate")
    public JdbcTemplate pgVectorJdbcTemplate(@Qualifier("pgVectorDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean("pgVectorSqlSessionFactory")
    public SqlSessionFactoryBean pgVectorSqlSessionFactory(@Qualifier("pgVectorDataSource") DataSource pgVectorDataSource) throws Exception {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(pgVectorDataSource);

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        sqlSessionFactoryBean.setMapperLocations(resolver.getResources("classpath:/mapper/pgvector/*.xml"));

        return sqlSessionFactoryBean;
    }

    @Bean("pgVectorSqlSessionTemplate")
    public SqlSessionTemplate pgVectorSqlSessionTemplate(@Qualifier("pgVectorSqlSessionFactory") SqlSessionFactoryBean factory) throws Exception {
        return new SqlSessionTemplate(Objects.requireNonNull(factory.getObject()));
    }

    public PlatformTransactionManager pgVectorTransactionManager(@Qualifier("pgVectorDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
