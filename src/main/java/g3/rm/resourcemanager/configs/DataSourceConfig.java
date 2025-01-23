package g3.rm.resourcemanager.configs;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.state")
    public DataSourceProperties stateDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean
    public DataSource stateDataSource() {
        return stateDataSourceProperties()
                .initializeDataSourceBuilder()
                .build();
    }

    @Primary
    @Bean
    public NamedParameterJdbcTemplate stateJdbcTemplate() {
        return new NamedParameterJdbcTemplate(stateDataSource());
    }

    @Bean
    @ConfigurationProperties("spring.datasource.inner")
    public DataSourceProperties innerDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource innerDataSource() {
        return innerDataSourceProperties()
                .initializeDataSourceBuilder()
                .build();
    }

    @Bean
    public NamedParameterJdbcTemplate innerJdbcTemplate() {
        return new NamedParameterJdbcTemplate(innerDataSource());
    }
}
