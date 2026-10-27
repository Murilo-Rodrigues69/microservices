package isakids.isa.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.integracoes.fiscal")
public class FiscalIntegrationProperties {

    private String baseUrl = "http://localhost:8081";
    private String token;
    private int connectTimeoutSeconds = 5;
    private int readTimeoutSeconds = 30;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getConnectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    public void setConnectTimeoutSeconds(int connectTimeoutSeconds) {
        this.connectTimeoutSeconds = connectTimeoutSeconds;
    }

    public int getReadTimeoutSeconds() {
        return readTimeoutSeconds;
    }

    public void setReadTimeoutSeconds(int readTimeoutSeconds) {
        this.readTimeoutSeconds = readTimeoutSeconds;
    }
}
