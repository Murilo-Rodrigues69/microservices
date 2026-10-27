package isakids.nfcelab.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import isakids.nfcelab.NfceLabApplication;

@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Bean
    DataSource dataSource(DataSourceProperties properties, Environment environment) {
        String configuredUrl = properties.getUrl();
        boolean autoH2 = !StringUtils.hasText(configuredUrl);
        String url = autoH2 ? buildDefaultH2Url(environment) : configuredUrl;

        properties.setUrl(url);
        if (autoH2) {
            properties.setUsername(environment.getProperty("app.h2.username", "sa"));
            properties.setPassword(environment.getProperty("app.h2.password", ""));
        }

        if (!StringUtils.hasText(properties.getDriverClassName()) && url.startsWith("jdbc:h2:")) {
            properties.setDriverClassName("org.h2.Driver");
        }

        logResolvedDatasource(url, properties.getUsername());
        return properties.initializeDataSourceBuilder().build();
    }

    private String buildDefaultH2Url(Environment environment) {
        String configuredPath = environment.getProperty("app.h2.path");
        Path h2Path = StringUtils.hasText(configuredPath) ? resolveConfiguredH2Path(configuredPath) : resolveDefaultH2Path();
        Path resolvedPath = h2Path.toAbsolutePath().normalize();
        return "jdbc:h2:file:" + resolvedPath.toString().replace('\\', '/') + ";MODE=PostgreSQL;AUTO_SERVER=TRUE";
    }

    private Path resolveDefaultH2Path() {
        Path applicationDir = new ApplicationHome(NfceLabApplication.class).getDir().toPath().toAbsolutePath().normalize();
        Path baseDir = resolveBaseDir(applicationDir);
        return baseDir.resolve("data").resolve("isa-nfce-lab");
    }

    private Path resolveConfiguredH2Path(String configuredPath) {
        Path path = Paths.get(configuredPath);
        if (path.isAbsolute()) {
            return path;
        }
        Path applicationDir = new ApplicationHome(NfceLabApplication.class).getDir().toPath().toAbsolutePath().normalize();
        return resolveBaseDir(applicationDir).resolve(path);
    }

    private Path resolveBaseDir(Path applicationDir) {
        Path fileName = applicationDir.getFileName();
        if (fileName != null && "classes".equals(fileName.toString())) {
            Path parent = applicationDir.getParent();
            Path grandParent = parent != null ? parent.getParent() : null;
            if (parent != null && grandParent != null && "target".equals(parent.getFileName().toString())) {
                return grandParent;
            }
        }

        if (fileName != null && "target".equals(fileName.toString())) {
            Path parent = applicationDir.getParent();
            if (parent != null && Files.exists(parent.resolve("pom.xml"))) {
                return parent;
            }
        }

        return applicationDir;
    }

    private void logResolvedDatasource(String url, String username) {
        if (!StringUtils.hasText(url)) {
            return;
        }

        if (url.startsWith("jdbc:h2:file:")) {
            String path = url.substring("jdbc:h2:file:".length());
            int optionsStart = path.indexOf(';');
            if (optionsStart >= 0) {
                path = path.substring(0, optionsStart);
            }
            log.info("Using H2 datasource at {} with user {}", Paths.get(path).toAbsolutePath().normalize(), username);
            return;
        }

        log.info("Using datasource {} with user {}", url, username);
    }
}
