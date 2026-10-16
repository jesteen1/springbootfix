package in.kumarelectrical.contractor.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${DATABASE_URL:${DB_URL:${spring.datasource.url:}}}")
    private String configuredUrl;

    @Value("${DB_USER:${DB_USERNAME:${Db_user:${spring.datasource.username:postgres}}}}")
    private String configuredUsername;

    @Value("${DB_PASSWORD:${db_password:${spring.datasource.password:postgres}}}}")
    private String configuredPassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        String rawUrl = cleanString(configuredUrl);
        String username = cleanString(configuredUsername);
        String password = cleanString(configuredPassword);

        String jdbcUrl;

        if (StringUtils.hasText(rawUrl)) {
            if (rawUrl.startsWith("postgres://") || rawUrl.startsWith("postgresql://")) {
                try {
                    URI uri = URI.create(rawUrl);
                    String userInfo = uri.getUserInfo();
                    if (userInfo != null) {
                        String[] parts = userInfo.split(":", 2);
                        if (parts.length > 0 && !StringUtils.hasText(username)) {
                            username = parts[0];
                        }
                        if (parts.length > 1 && !StringUtils.hasText(password)) {
                            password = parts[1];
                        }
                    }

                    int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                    String host = uri.getHost();
                    String path = uri.getPath();
                    if (!StringUtils.hasText(path) || "/".equals(path)) {
                        path = "/kumar_electrical";
                    }

                    String query = uri.getQuery();
                    StringBuilder queryBuilder = new StringBuilder();
                    if (StringUtils.hasText(query)) {
                        queryBuilder.append("?").append(query);
                    } else if (host != null && host.contains(".render.com")) {
                        // Render external database connections require SSL
                        queryBuilder.append("?sslmode=require");
                    }

                    jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path + queryBuilder;
                } catch (Exception e) {
                    log.warn("Failed to parse DATABASE_URL as URI, using regex fallback: {}", e.getMessage());
                    jdbcUrl = rawUrl.replaceFirst("^postgres(ql)?://", "jdbc:postgresql://");
                }
            } else if (rawUrl.startsWith("jdbc:")) {
                jdbcUrl = rawUrl;
            } else {
                jdbcUrl = "jdbc:postgresql://" + rawUrl;
            }
        } else {
            jdbcUrl = "jdbc:postgresql://localhost:5432/kumar_electrical";
        }

        log.info("Configuring DataSource with JDBC URL: {}", maskJdbcUrl(jdbcUrl));

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        if (StringUtils.hasText(username)) {
            config.setUsername(username);
        }
        if (StringUtils.hasText(password)) {
            config.setPassword(password);
        }

        if (jdbcUrl.startsWith("jdbc:postgresql:")) {
            config.setDriverClassName("org.postgresql.Driver");
        } else if (jdbcUrl.startsWith("jdbc:mysql:")) {
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        }

        // Production-ready connection pool tuning for cloud deployments (e.g. Render)
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setKeepaliveTime(60000);

        return new HikariDataSource(config);
    }

    private String cleanString(String input) {
        if (!StringUtils.hasText(input)) {
            return "";
        }
        String trimmed = input.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        } else if (trimmed.startsWith("'") && trimmed.endsWith("'") && trimmed.length() >= 2) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    private String maskJdbcUrl(String url) {
        if (!StringUtils.hasText(url)) return "";
        // If password is embedded in the URL, mask it for logs
        return url.replaceAll("(?i):([^:@]+)@", ":****@");
    }
}
