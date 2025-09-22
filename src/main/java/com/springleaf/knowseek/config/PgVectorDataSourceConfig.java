package com.springleaf.knowseek.config;

import com.zaxxer.hikari.HikariDataSource;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Objects;

@Configuration
@MapperScan(
        basePackages = "com.springleaf.knowseek.mapper.pgvector",
        sqlSessionTemplateRef = "pgVectorSqlSessionTemplate"
)
public class PgVectorDataSourceConfig {

    @Bean("pgVectorDataSource")
    public DataSource pgVectorDataSource(
            @Value("${spring.datasource.pgvector.driver-class-name}") String driverClassName,
            @Value("${spring.datasource.pgvector.url}") String url,
            @Value("${spring.datasource.pgvector.username}") String username,
            @Value("${spring.datasource.pgvector.password}") String password,
            @Value("${spring.datasource.pgvector.hikari.maximum-pool-size:5}") int maximumPoolSize,
            @Value("${spring.datasource.pgvector.hikari.minimum-idle:2}") int minimumIdle,
            @Value("${spring.datasource.pgvector.hikari.idle-timeout:30000}") long idleTimeout,
            @Value("${spring.datasource.pgvector.hikari.connection-timeout:30000}") long connectionTimeout) {

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(driverClassName);
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        dataSource.setMaximumPoolSize(maximumPoolSize);
        dataSource.setMinimumIdle(minimumIdle);
        dataSource.setIdleTimeout(idleTimeout);
        dataSource.setConnectionTimeout(connectionTimeout);

        dataSource.setInitializationFailTimeout(1);
        dataSource.setConnectionTestQuery("SELECT 1");
        dataSource.setAutoCommit(true);
        dataSource.setPoolName("PgVectorHikariPool");

        return dataSource;
    }

    @Bean("pgVectorJdbcTemplate")
    public JdbcTemplate pgVectorJdbcTemplate(@Qualifier("pgVectorDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean("pgVectorSqlSessionFactory")
    public SqlSessionFactoryBean pgVectorSqlSessionFactory(@Qualifier("pgVectorDataSource") DataSource pgVectorDataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(pgVectorDataSource);
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath:/mapper/pgvector/*.xml"));
        return factoryBean;
    }

    @Bean("pgVectorSqlSessionTemplate")
    public SqlSessionTemplate pgVectorSqlSessionTemplate(@Qualifier("pgVectorSqlSessionFactory") SqlSessionFactoryBean factory) throws Exception {
        return new SqlSessionTemplate(Objects.requireNonNull(factory.getObject()));
    }

    @Bean("pgVectorTransactionManager")
    public PlatformTransactionManager pgVectorTransactionManager(@Qualifier("pgVectorDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}