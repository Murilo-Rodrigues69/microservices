package isakids.isa.services;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import isakids.isa.config.WhatsappIntegrationProperties;
import isakids.isa.dto.WhatsappSendNfceResponse;
import isakids.isa.entities.Venda;

@Service
public class WhatsappService {

    private static final Logger log = LoggerFactory.getLogger(WhatsappService.class);
    private static final String WHATSAPP_PRODUCT = "whatsapp";
    private static final String PDF_MIME_TYPE = "application/pdf";

    private final WhatsappIntegrationProperties properties;
    private final VendaService vendaService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public WhatsappService(WhatsappIntegrationProperties properties, VendaService vendaService) {
        this.properties = properties;
        this.vendaService = vendaService;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(1, properties.getConnectTimeoutSeconds())))
                .build();
    }

    public WhatsappSendNfceResponse enviarNfce(Long vendaId, String telefone, String legenda) {
        try {
            Venda venda = vendaService.buscarPorId(vendaId);
            byte[] pdf = vendaService.carregarPdfNfce(vendaId);
            String telefoneNormalizado = normalizeTelefone(telefone);
            String fileName = resolveFileName(venda);
            String mediaId = uploadDocument(pdf, fileName);
            String messageId = sendDocumentMessage(telefoneNormalizado, mediaId, fileName,
                    StringUtils.hasText(legenda) ? legenda.trim() : buildDefaultCaption(venda));

            return new WhatsappSendNfceResponse(vendaId, telefoneNormalizado, mediaId, messageId, "QUEUED");
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao enviar NFC pelo WhatsApp: " + e.getMessage(), e);
        }
    }

    public String validarWebhook(String mode, String verifyToken, String challenge) {
        if (!"subscribe".equals(mode)) {
            throw new AccessDeniedException("Webhook do WhatsApp com hub.mode invalido.");
        }
        if (!StringUtils.hasText(challenge)) {
            throw new IllegalArgumentException("hub.challenge nao foi informado.");
        }

        String configuredVerifyToken = requireConfiguredValue(properties.getVerifyToken(),
                "app.integracoes.whatsapp.verify-token");
        if (!configuredVerifyToken.equals(verifyToken)) {
            throw new AccessDeniedException("Webhook do WhatsApp com verify token invalido.");
        }

        return challenge;
    }

    public Map<String, Object> processarWebhook(String signature, String payload) {
        String safePayload = payload != null ? payload : "";
        validateSignatureIfConfigured(signature, safePayload);

        try {
            JsonNode root = safePayload.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(safePayload);
            int entryCount = root.path("entry").isArray() ? root.path("entry").size() : 0;
            int messageCount = countNestedArrayItems(root, "messages");
            int statusCount = countNestedArrayItems(root, "statuses");

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("received", true);
            response.put("object", root.path("object").asText(null));
            response.put("entryCount", entryCount);
            response.put("messageCount", messageCount);
            response.put("statusCount", statusCount);

            log.info("Webhook WhatsApp recebido: object={}, entries={}, messages={}, statuses={}",
                    response.get("object"), entryCount, messageCount, statusCount);
            return response;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao processar webhook do WhatsApp: " + e.getMessage(), e);
        }
    }

    private String uploadDocument(byte[] pdfBytes, String fileName) throws Exception {
        String boundary = "----IsaWhatsappBoundary" + UUID.randomUUID();
        HttpRequest request = authorizedRequest(resolveGraphUri("/" + requirePhoneNumberId() + "/media"))
                .timeout(Duration.ofSeconds(Math.max(1, properties.getReadTimeoutSeconds())))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(buildMultipartBody(boundary, pdfBytes, fileName)))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        ensureSuccess(response.statusCode(), response.body(), "upload da NFC para o WhatsApp");

        JsonNode node = objectMapper.readTree(response.body());
        String mediaId = trimToNull(node.path("id").asText());
        if (!StringUtils.hasText(mediaId)) {
            throw new IllegalStateException("WhatsApp nao retornou media id no upload da NFC.");
        }
        return mediaId;
    }

    private String sendDocumentMessage(String telefone, String mediaId, String fileName, String caption) throws Exception {
        Map<String, Object> body = Map.of(
                "messaging_product", WHATSAPP_PRODUCT,
                "recipient_type", "individual",
                "to", telefone,
                "type", "document",
                "document", buildDocumentPayload(mediaId, fileName, caption));

        HttpRequest request = authorizedRequest(resolveGraphUri("/" + requirePhoneNumberId() + "/messages"))
                .timeout(Duration.ofSeconds(Math.max(1, properties.getReadTimeoutSeconds())))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        ensureSuccess(response.statusCode(), response.body(), "envio da NFC pelo WhatsApp");

        JsonNode node = objectMapper.readTree(response.body());
        JsonNode messages = node.path("messages");
        if (!messages.isArray() || messages.isEmpty()) {
            throw new IllegalStateException("WhatsApp nao retornou message id no envio da NFC.");
        }
        String messageId = trimToNull(messages.get(0).path("id").asText());
        if (!StringUtils.hasText(messageId)) {
            throw new IllegalStateException("WhatsApp nao retornou message id no envio da NFC.");
        }
        return messageId;
    }

    private Map<String, Object> buildDocumentPayload(String mediaId, String fileName, String caption) {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("id", mediaId);
        document.put("filename", fileName);
        if (StringUtils.hasText(caption)) {
            document.put("caption", caption.trim());
        }
        return document;
    }

    private HttpRequest.Builder authorizedRequest(URI uri) {
        return HttpRequest.newBuilder()
                .uri(uri)
                .header("Authorization", "Bearer " + requireAccessToken());
    }

    private byte[] buildMultipartBody(String boundary, byte[] pdfBytes, String fileName) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writeTextPart(output, boundary, "messaging_product", WHATSAPP_PRODUCT);
        output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n")
                .getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Type: " + PDF_MIME_TYPE + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(pdfBytes);
        output.write("\r\n".getBytes(StandardCharsets.UTF_8));
        output.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return output.toByteArray();
    }

    private void writeTextPart(ByteArrayOutputStream output, String boundary, String fieldName, String value)
            throws Exception {
        output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Disposition: form-data; name=\"" + fieldName + "\"\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8));
        output.write(value.getBytes(StandardCharsets.UTF_8));
        output.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private void validateSignatureIfConfigured(String signature, String payload) {
        if (!StringUtils.hasText(properties.getAppSecret())) {
            return;
        }
        if (!StringUtils.hasText(signature)) {
            throw new AccessDeniedException("Webhook do WhatsApp sem assinatura.");
        }

        String prefix = "sha256=";
        if (!signature.startsWith(prefix)) {
            throw new AccessDeniedException("Webhook do WhatsApp com assinatura invalida.");
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.getAppSecret().trim().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            byte[] received = hexToBytes(signature.substring(prefix.length()));

            if (!MessageDigest.isEqual(expected, received)) {
                throw new AccessDeniedException("Webhook do WhatsApp com assinatura invalida.");
            }
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao validar assinatura do webhook do WhatsApp.", e);
        }
    }

    private int countNestedArrayItems(JsonNode node, String fieldName) {
        int count = 0;
        JsonNode entries = node.path("entry");
        if (!entries.isArray()) {
            return 0;
        }

        for (JsonNode entry : entries) {
            JsonNode changes = entry.path("changes");
            if (!changes.isArray()) {
                continue;
            }
            for (JsonNode change : changes) {
                JsonNode nested = change.path("value").path(fieldName);
                if (nested.isArray()) {
                    count += nested.size();
                }
            }
        }
        return count;
    }

    private URI resolveGraphUri(String path) {
        String baseUrl = requireConfiguredValue(properties.getBaseUrl(), "app.integracoes.whatsapp.base-url");
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String apiVersion = trimToNull(properties.getApiVersion());
        String normalizedVersion = apiVersion == null ? "" : "/" + apiVersion;
        return URI.create(normalizedBaseUrl + normalizedVersion + path);
    }

    private void ensureSuccess(int statusCode, String body, String action) {
        if (statusCode >= 200 && statusCode < 300) {
            return;
        }
        String detail = StringUtils.hasText(body) ? body : "sem corpo de resposta";
        throw new IllegalStateException("Falha ao " + action + ". HTTP " + statusCode + ": " + detail);
    }

    private String resolveFileName(Venda venda) {
        if (StringUtils.hasText(venda.getNotaFiscalChaveAcesso())) {
            return venda.getNotaFiscalChaveAcesso().trim() + ".pdf";
        }
        return "nfce-venda-" + venda.getId() + ".pdf";
    }

    private String buildDefaultCaption(Venda venda) {
        StringBuilder caption = new StringBuilder("NFC-e da venda #").append(venda.getId());
        if (venda.getLoja() != null && StringUtils.hasText(venda.getLoja().getNome())) {
            caption.append(" - ").append(venda.getLoja().getNome().trim());
        }
        return caption.toString();
    }

    private String normalizeTelefone(String telefone) {
        if (!StringUtils.hasText(telefone)) {
            throw new IllegalArgumentException("telefone e obrigatorio.");
        }
        String normalized = telefone.replaceAll("\\D", "");
        if (normalized.length() < 10 || normalized.length() > 15) {
            throw new IllegalArgumentException("telefone deve estar em formato internacional com 10 a 15 digitos.");
        }
        return normalized;
    }

    private String requireAccessToken() {
        return requireConfiguredValue(properties.getAccessToken(), "app.integracoes.whatsapp.access-token");
    }

    private String requirePhoneNumberId() {
        return requireConfiguredValue(properties.getPhoneNumberId(), "app.integracoes.whatsapp.phone-number-id");
    }

    private String requireConfiguredValue(String value, String propertyName) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new IllegalStateException(propertyName + " nao foi configurada.");
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private byte[] hexToBytes(String value) {
        String normalized = trimToNull(value);
        if (normalized == null || normalized.length() % 2 != 0) {
            throw new IllegalArgumentException("Assinatura hexadecimal invalida.");
        }

        byte[] bytes = new byte[normalized.length() / 2];
        for (int i = 0; i < normalized.length(); i += 2) {
            bytes[i / 2] = (byte) Integer.parseInt(normalized.substring(i, i + 2), 16);
        }
        return bytes;
    }
}
