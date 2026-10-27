package isakids.nfcelab.document;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import isakids.nfcelab.config.NfceDocumentProperties;
import tools.jackson.databind.ObjectMapper;

@Component
public class MeuDanfeClient {

    private final NfceDocumentProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public MeuDanfeClient(NfceDocumentProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, HttpClient.newHttpClient());
    }

    MeuDanfeClient(NfceDocumentProperties properties, ObjectMapper objectMapper, HttpClient httpClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public boolean isConfigured() {
        return StringUtils.hasText(properties.getMeuDanfe().getApiKey());
    }

    public byte[] gerarDanfePdf(String xml) {
        String apiKey = properties.getMeuDanfe().getApiKey();
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("Configure app.nfce.documentos.meudanfe.api-key para usar o Meu Danfe.");
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizeBaseUrl(properties.getMeuDanfe().getBaseUrl()) + "/fd/convert/xml-to-da"))
                    .timeout(Duration.ofSeconds(Math.max(properties.getMeuDanfe().getTimeoutSeconds(), 5)))
                    .header("Api-Key", apiKey.trim())
                    .header("Content-Type", "text/plain; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(xml, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Meu Danfe retornou HTTP " + response.statusCode() + ": " + truncate(response.body(), 500));
            }

            MeuDanfeBase64Response payload = objectMapper.readValue(response.body(), MeuDanfeBase64Response.class);
            if (payload == null || !StringUtils.hasText(payload.data())) {
                throw new IllegalStateException("Meu Danfe nao retornou o PDF em Base64.");
            }

            return Base64.getDecoder().decode(payload.data());
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao gerar DANFE real via Meu Danfe.", e);
        }
    }

    public Optional<MeuDanfeFetchedDocument> baixarDocumentosPorChave(String chaveAcesso) {
        String normalizedChave = onlyDigits(chaveAcesso);
        if (!StringUtils.hasText(normalizedChave) || normalizedChave.length() != 44) {
            throw new IllegalArgumentException("Chave de acesso invalida para consulta no Meu Danfe.");
        }

        SearchResponse status = aguardarDisponibilidade(normalizedChave);
        if (status == null || !"OK".equalsIgnoreCase(status.status())) {
            return Optional.empty();
        }

        String xml = baixarXmlPorChave(normalizedChave);
        byte[] pdf = baixarPdfPorChave(normalizedChave);
        if (!StringUtils.hasText(xml) || pdf == null || pdf.length == 0) {
            return Optional.empty();
        }

        return Optional.of(new MeuDanfeFetchedDocument(xml, pdf));
    }

    private SearchResponse aguardarDisponibilidade(String chaveAcesso) {
        final int maxTentativas = 12;
        for (int tentativa = 0; tentativa < maxTentativas; tentativa++) {
            SearchResponse response = solicitarDocumentoPorChave(chaveAcesso);
            if (response == null) {
                return null;
            }

            String status = response.status();
            if ("OK".equalsIgnoreCase(status) || "NOT_FOUND".equalsIgnoreCase(status) || "ERROR".equalsIgnoreCase(status)) {
                return response;
            }

            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Thread interrompida aguardando resposta do Meu Danfe.", e);
            }
        }

        return solicitarDocumentoPorChave(chaveAcesso);
    }

    private SearchResponse solicitarDocumentoPorChave(String chaveAcesso) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizeBaseUrl(properties.getMeuDanfe().getBaseUrl()) + "/fd/add/" + chaveAcesso))
                    .timeout(Duration.ofSeconds(Math.max(properties.getMeuDanfe().getTimeoutSeconds(), 5)))
                    .header("Api-Key", requireApiKey())
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Meu Danfe retornou HTTP " + response.statusCode() + " ao consultar chave: "
                        + truncate(response.body(), 500));
            }

            return objectMapper.readValue(response.body(), SearchResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException("Falha consultando chave no Meu Danfe.", e);
        }
    }

    private String baixarXmlPorChave(String chaveAcesso) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizeBaseUrl(properties.getMeuDanfe().getBaseUrl()) + "/fd/get/xml/" + chaveAcesso))
                    .timeout(Duration.ofSeconds(Math.max(properties.getMeuDanfe().getTimeoutSeconds(), 5)))
                    .header("Api-Key", requireApiKey())
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Meu Danfe retornou HTTP " + response.statusCode() + " ao baixar XML: "
                        + truncate(response.body(), 500));
            }

            MeuDanfeTextResponse payload = objectMapper.readValue(response.body(), MeuDanfeTextResponse.class);
            return payload != null ? payload.data() : null;
        } catch (Exception e) {
            throw new IllegalStateException("Falha baixando XML via Meu Danfe.", e);
        }
    }

    private byte[] baixarPdfPorChave(String chaveAcesso) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizeBaseUrl(properties.getMeuDanfe().getBaseUrl()) + "/fd/get/da/" + chaveAcesso))
                    .timeout(Duration.ofSeconds(Math.max(properties.getMeuDanfe().getTimeoutSeconds(), 5)))
                    .header("Api-Key", requireApiKey())
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Meu Danfe retornou HTTP " + response.statusCode() + " ao baixar PDF: "
                        + truncate(response.body(), 500));
            }

            MeuDanfeBase64Response payload = objectMapper.readValue(response.body(), MeuDanfeBase64Response.class);
            if (payload == null || !StringUtils.hasText(payload.data())) {
                return null;
            }
            return Base64.getDecoder().decode(payload.data());
        } catch (Exception e) {
            throw new IllegalStateException("Falha baixando PDF via Meu Danfe.", e);
        }
    }

    private String requireApiKey() {
        String apiKey = properties.getMeuDanfe().getApiKey();
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("Configure app.nfce.documentos.meudanfe.api-key para usar o Meu Danfe.");
        }
        return apiKey.trim();
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = StringUtils.hasText(baseUrl) ? baseUrl.trim() : "https://api.meudanfe.com.br/v2";
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

    private String onlyDigits(String value) {
        return value == null ? null : value.replaceAll("\\D", "");
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MeuDanfeBase64Response(String name, String type, String format, String data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MeuDanfeTextResponse(String name, String type, String format, String data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SearchResponse(String value, String type, String status, String statusMessage) {
    }

    public record MeuDanfeFetchedDocument(String xml, byte[] pdf) {
    }
}
