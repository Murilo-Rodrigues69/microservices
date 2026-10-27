package isakids.nfcelab.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebCorsConfig implements WebMvcConfigurer {

    private static final List<String> DEFAULT_ALLOWED_ORIGINS = List.of(
            "http://localhost:5000",
            "http://127.0.0.1:5000");

    private final List<String> allowedOrigins;

    public WebCorsConfig(@Value("${app.cors.allowed-origins:}") String allowedOrigins) {
        List<String> parsedOrigins = parseAllowedOrigins(allowedOrigins);
        this.allowedOrigins = parsedOrigins.isEmpty() ? DEFAULT_ALLOWED_ORIGINS : parsedOrigins;
        if (this.allowedOrigins.contains("*")) {
            throw new IllegalStateException("app.cors.allowed-origins nao pode usar * com credenciais habilitadas.");
        }
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders(HttpHeaders.CONTENT_DISPOSITION)
                .allowCredentials(true)
                .maxAge(3600);
    }

    private List<String> parseAllowedOrigins(String rawOrigins) {
        if (!StringUtils.hasText(rawOrigins)) {
            return List.of();
        }
        return Arrays.stream(rawOrigins.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }
}
