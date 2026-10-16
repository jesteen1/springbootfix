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

    private static final String DEFAULT_RENDER_URL = "jdbc:postgresql://dpg-db8mbulg1s2s7382fjs0-a:5432/healthmoniter";
    private static final String DEFAULT_RENDER_USER = "healthmoniter_user";
    private static final String DEFAULT_RENDER_PASS = "x9jq2q7LCYnYS2RTOnE8WbeXTWRLG9WL";

    @Value("${DATABASE_URL:${DB_URL:${spring.datasource.url:}}}")
    private String configuredUrl;

    @Value("${DB_USER:${DB_USERNAME:${Db_user:${spring.datasource.username:}}}}")
    private String configuredUsername;

    @Value("${DB_PASSWORD:${db_password:${spring.datasource.password:}}}}")
    private String configuredPassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        String rawUrl = cleanString(configuredUrl);
        String username = cleanString(configuredUsername);
        String password = cleanString(configuredPassword);

        boolean isRender = System.getenv("RENDER") != null;

        // If no URL or pointing to localhost while running on Render, use the user's Render database
        if (!StringUtils.hasText(rawUrl) || (isRender && rawUrl.contains("localhost:5432"))) {
            if (isRender && rawUrl.contains("localhost:5432")) {
                log.warn("Detected running on Render with 'localhost:5432'. Redirecting to Render PostgreSQL instance: {}", DEFAULT_RENDER_URL);
            }
            rawUrl = DEFAULT_RENDER_URL;
        }

        String jdbcUrl;

        if (rawUrl.startsWith("postgres://") || rawUrl.startsWith("postgresql://")) {
            try {
                URI uri = URI.create(rawUrl);
                String userInfo = uri.getUserInfo();
                if (userInfo != null) {
                    String[] parts = userInfo.split(":", 2);
                    if (parts.length > 0 && StringUtils.hasText(parts[0])) {
                        username = parts[0];
                    }
                    if (parts.length > 1 && StringUtils.hasText(parts[1])) {
                        password = parts[1];
                    }
                }

                int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                String host = uri.getHost();
                String path = uri.getPath();
                if (!StringUtils.hasText(path) || "/".equals(path)) {
                    path = "/healthmoniter";
                }

                String query = uri.getQuery();
                StringBuilder queryBuilder = new StringBuilder();
                if (StringUtils.hasText(query)) {
                    queryBuilder.append("?").append(query);
                } else if (host != null && host.contains(".render.com")) {
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

        // Clean user/pass if embedded in jdbc:postgresql://user:pass@host...
        if (jdbcUrl.startsWith("jdbc:postgresql://") && jdbcUrl.contains("@")) {
            try {
                String withoutPrefix = jdbcUrl.substring("jdbc:postgresql://".length());
                int atIndex = withoutPrefix.indexOf('@');
                if (atIndex > 0) {
                    String userPass = withoutPrefix.substring(0, atIndex);
                    String rest = withoutPrefix.substring(atIndex + 1);
                    String[] upParts = userPass.split(":", 2);
                    if (upParts.length > 0 && StringUtils.hasText(upParts[0])) username = upParts[0];
                    if (upParts.length > 1 && StringUtils.hasText(upParts[1])) password = upParts[1];
                    jdbcUrl = "jdbc:postgresql://" + rest;
                }
            } catch (Exception ignored) {}
        }

        // If username is "postgres" or blank on Render, substitute the correct Render database username & password
        if (!StringUtils.hasText(username) || "postgres".equalsIgnoreCase(username)) {
            username = DEFAULT_RENDER_USER;
        }
        if (!StringUtils.hasText(password) || "postgres".equalsIgnoreCase(password)) {
            password = DEFAULT_RENDER_PASS;
        }

        log.info("Configuring DataSource with JDBC URL: {} | User: {}", maskJdbcUrl(jdbcUrl), username);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);

        if (jdbcUrl.startsWith("jdbc:postgresql:")) {
            config.setDriverClassName("org.postgresql.Driver");
        } else if (jdbcUrl.startsWith("jdbc:mysql:")) {
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        }

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
        return url.replaceAll("(?i):([^:@]+)@", ":****@");
    }
}
