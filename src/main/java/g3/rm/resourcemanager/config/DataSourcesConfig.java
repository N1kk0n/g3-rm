package g3.rm.resourcemanager.config;

import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@Configuration
public class DataSourcesConfig {
    @Autowired
    private Environment environment;

    @Bean
    public NamedParameterJdbcTemplate template() {
        final String HOST_PORT = "host.port";
        final String ROOT_CERT = "root.cert";
        final String USER_CERT = "user.cert";
        final String USER_KEY = "user.key.pk8";

        final String DB_NAME = "g3";

        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder
                .append("jdbc:postgresql://")
                .append(environment.getProperty(HOST_PORT))
                .append("/")
                .append(DB_NAME)
                .append("?")
                .append("user=g3rm")
                .append("&")
                .append("ssl=true")
                .append("&")
                .append("sslmode=verify-full")
                .append("&")
                .append("sslrootcert=").append(environment.getProperty(ROOT_CERT))
                .append("&")
                .append("sslcert=").append(environment.getProperty(USER_CERT))
                .append("&")
                .append("sslkey=").append(environment.getProperty(USER_KEY));

        DataSourceBuilder<?> builder = DataSourceBuilder.create();
        builder.type(PGSimpleDataSource.class);
        builder.driverClassName("org.postgresql.Driver");
        builder.url(urlBuilder.toString());
        return new NamedParameterJdbcTemplate(builder.build());
    }
}
