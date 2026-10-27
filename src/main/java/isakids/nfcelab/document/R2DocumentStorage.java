package isakids.nfcelab.document;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import isakids.nfcelab.config.NfceDocumentProperties;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
public class R2DocumentStorage implements FiscalDocumentStorage, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(R2DocumentStorage.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final NfceDocumentProperties properties;
    private final S3Client s3Client;

    @Autowired
    public R2DocumentStorage(NfceDocumentProperties properties) {
        this.properties = properties;
        this.s3Client = buildClient(properties);
    }

    @Override
    public boolean isEnabled() {
        return s3Client != null;
    }

    @Override
    public void store(String chaveAcesso, String xml, byte[] pdfBytes) {
        if (!isEnabled()) {
            return;
        }

        s3Client.putObject(PutObjectRequest.builder()
                        .bucket(bucket())
                        .key(xmlKey(chaveAcesso))
                        .contentType("application/xml")
                        .build(),
                RequestBody.fromString(xml, StandardCharsets.UTF_8));
        s3Client.putObject(PutObjectRequest.builder()
                        .bucket(bucket())
                        .key(pdfKey(chaveAcesso))
                        .contentType("application/pdf")
                        .build(),
                RequestBody.fromBytes(pdfBytes));
    }

    @Override
    public Optional<NfceStoredDocument> load(String chaveAcesso) {
        if (!isEnabled()) {
            return Optional.empty();
        }

        try {
            ResponseBytes<GetObjectResponse> xmlBytes = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(bucket())
                    .key(xmlKey(chaveAcesso))
                    .build());
            ResponseBytes<GetObjectResponse> pdfBytes = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(bucket())
                    .key(pdfKey(chaveAcesso))
                    .build());
            return Optional.of(new NfceStoredDocument(
                    xmlBytes.asUtf8String(),
                    pdfBytes.asByteArray()));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    @Override
    public void close() {
        if (s3Client != null) {
            try {
                s3Client.close();
            } catch (Exception e) {
                log.debug("Falha ao encerrar cliente S3 do R2.", e);
            }
        }
    }

    private String bucket() {
        return properties.getR2().getBucket().trim();
    }

    private String xmlKey(String chaveAcesso) {
        return resolveKey(chaveAcesso, "xml");
    }

    private String pdfKey(String chaveAcesso) {
        return resolveKey(chaveAcesso, "pdf");
    }

    private String resolveKey(String chaveAcesso, String extensao) {
        String prefix = normalizePrefix(properties.getR2().getKeyPrefix());
        if (!StringUtils.hasText(prefix)) {
            return chaveAcesso + "." + extensao;
        }
        return prefix + "/" + chaveAcesso + "." + extensao;
    }

    private static String normalizePrefix(String prefix) {
        if (!StringUtils.hasText(prefix)) {
            return "";
        }
        String normalized = prefix.trim().replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static S3Client buildClient(NfceDocumentProperties properties) {
        NfceDocumentProperties.R2 r2 = properties.getR2();
        if (r2 == null || !r2.isEnabled() || !StringUtils.hasText(r2.getBucket())) {
            return null;
        }

        String endpoint = resolveEndpoint(r2);
        AwsBasicCredentials credentials = resolveCredentials(r2);
        if (!StringUtils.hasText(endpoint) || credentials == null) {
            return null;
        }

        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(StringUtils.hasText(r2.getRegion()) ? r2.getRegion().trim() : "auto"))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    private static String resolveEndpoint(NfceDocumentProperties.R2 r2) {
        if (StringUtils.hasText(r2.getEndpoint())) {
            return r2.getEndpoint().trim();
        }
        if (StringUtils.hasText(r2.getAccountId())) {
            return "https://" + r2.getAccountId().trim() + ".r2.cloudflarestorage.com";
        }
        return null;
    }

    private static AwsBasicCredentials resolveCredentials(NfceDocumentProperties.R2 r2) {
        if (StringUtils.hasText(r2.getAccessKeyId()) && StringUtils.hasText(r2.getSecretAccessKey())) {
            return AwsBasicCredentials.create(r2.getAccessKeyId().trim(), r2.getSecretAccessKey().trim());
        }
        if (StringUtils.hasText(r2.getApiToken()) && StringUtils.hasText(r2.getAccountId())) {
            String apiToken = r2.getApiToken().trim();
            String tokenId = fetchTokenId(r2.getAccountId().trim(), apiToken);
            return AwsBasicCredentials.create(tokenId, sha256Hex(apiToken));
        }
        return null;
    }

    private static String fetchTokenId(String accountId, String apiToken) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.cloudflare.com/client/v4/accounts/" + accountId + "/tokens/verify"))
                    .header("Authorization", "Bearer " + apiToken)
                    .GET()
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("Falha verificando token do Cloudflare R2: HTTP " + response.statusCode());
            }
            JsonNode body = OBJECT_MAPPER.readTree(response.body());
            String tokenId = body.path("result").path("id").asText();
            if (!StringUtils.hasText(tokenId)) {
                throw new IllegalStateException("Resposta do Cloudflare R2 nao retornou o id do token.");
            }
            return tokenId.trim();
        } catch (Exception e) {
            throw new IllegalStateException("Falha derivando Access Key ID do token do Cloudflare R2.", e);
        }
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Falha calculando Secret Access Key do Cloudflare R2.", e);
        }
    }
}
