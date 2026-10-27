package isakids.nfcelab.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.nfce.documentos")
public class NfceDocumentProperties {

    private String outputDir = "documentos-fiscais";
    private PdfProvider pdfProvider = PdfProvider.AUTO;
    private MeuDanfe meuDanfe = new MeuDanfe();
    private R2 r2 = new R2();

    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public PdfProvider getPdfProvider() {
        return pdfProvider;
    }

    public void setPdfProvider(PdfProvider pdfProvider) {
        this.pdfProvider = pdfProvider;
    }

    public MeuDanfe getMeuDanfe() {
        return meuDanfe;
    }

    public void setMeuDanfe(MeuDanfe meuDanfe) {
        this.meuDanfe = meuDanfe;
    }

    public R2 getR2() {
        return r2;
    }

    public void setR2(R2 r2) {
        this.r2 = r2;
    }

    public enum PdfProvider {
        AUTO,
        LOCAL,
        MEUDANFE
    }

    public static class MeuDanfe {
        private String baseUrl = "https://api.meudanfe.com.br/v2";
        private String apiKey;
        private int timeoutSeconds = 30;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
    }

    public static class R2 {
        private boolean enabled;
        private String accountId;
        private String apiToken;
        private String endpoint;
        private String bucket;
        private String accessKeyId;
        private String secretAccessKey;
        private String region = "auto";
        private String keyPrefix = "notas";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getAccountId() {
            return accountId;
        }

        public void setAccountId(String accountId) {
            this.accountId = accountId;
        }

        public String getApiToken() {
            return apiToken;
        }

        public void setApiToken(String apiToken) {
            this.apiToken = apiToken;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getAccessKeyId() {
            return accessKeyId;
        }

        public void setAccessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
        }

        public String getSecretAccessKey() {
            return secretAccessKey;
        }

        public void setSecretAccessKey(String secretAccessKey) {
            this.secretAccessKey = secretAccessKey;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }
    }
}
