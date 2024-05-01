package g3.rm.resourcemanager.config;

import g3.rm.resourcemanager.datasources.Ceph;
import g3.rm.resourcemanager.datasources.OracleDB;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@Configuration
public class DataSourcesConfig {
    @Autowired
    private Environment environment;

    @Bean
    @Scope("prototype")
    Ceph ceph() {
        return new Ceph();
    }

    @Bean
    @Scope("prototype")
    OracleDB oracleDB() {
        return new OracleDB();
    }

    @Bean
    public NamedParameterJdbcTemplate template() {
        final String CONFIG_DB_URL="config.url";
        final String CONFIG_DB_USER="config.user";
        final String CONFIG_DB_PASSWORD="config.password";

        DataSourceBuilder<?> builder = DataSourceBuilder.create();
        builder.type(PGSimpleDataSource.class);
        builder.driverClassName("org.postgresql.Driver");
        builder.url(environment.getProperty(CONFIG_DB_URL));
        builder.username(environment.getProperty(CONFIG_DB_USER));
        builder.password(environment.getProperty(CONFIG_DB_PASSWORD));
        return new NamedParameterJdbcTemplate(builder.build());
    }
}
