package com.example.resumeinsight;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;

@Configuration
@org.springframework.context.annotation.Profile("!test")
public class DatabaseConfig {

    @Bean
    public DataSource dataSource() {
        String databaseUrl = System.getenv("DATABASE_URL");
        
        // If DATABASE_URL environment variable is provided (typical in Render deployments)
        if (databaseUrl != null && databaseUrl.startsWith("postgres://")) {
            try {
                URI dbUri = new URI(databaseUrl);
                String[] userInfo = dbUri.getUserInfo().split(":");
                String username = userInfo[0];
                String password = userInfo.length > 1 ? userInfo[1] : "";
                
                // Construct standard JDBC connection URL
                String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath();
                
                return DataSourceBuilder.create()
                        .url(dbUrl)
                        .username(username)
                        .password(password)
                        .driverClassName("org.postgresql.Driver")
                        .build();
            } catch (URISyntaxException e) {
                System.err.println("Error parsing DATABASE_URL: " + e.getMessage());
                // Let it fall back to standard settings
            }
        }
        
        // Fallback: Read standard Spring settings or environment hooks
        String jdbcUrl = System.getenv("SPRING_DATASOURCE_URL");
        if (jdbcUrl == null || jdbcUrl.isEmpty()) {
            jdbcUrl = "jdbc:postgresql://localhost:5432/resumeinsight";
        }
        
        String username = System.getenv("SPRING_DATASOURCE_USERNAME");
        if (username == null || username.isEmpty()) {
            username = "postgres";
        }
        
        String password = System.getenv("SPRING_DATASOURCE_PASSWORD");
        if (password == null || password.isEmpty()) {
            password = "postgres";
        }
        
        return DataSourceBuilder.create()
                .url(jdbcUrl)
                .username(username)
                .password(password)
                .driverClassName("org.postgresql.Driver")
                .build();
    }
}
