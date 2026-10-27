package isakids.isa.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import isakids.isa.config.WhatsappIntegrationProperties;
import isakids.isa.entities.Loja;
import isakids.isa.entities.Usuario;
import isakids.isa.entities.Venda;
import isakids.isa.enums.FormaPagamento;
import isakids.isa.enums.StatusVenda;

class WhatsappServiceTests {

    @Test
    void enviaNfceFazUploadDaMidiaEEnvioDaMensagem() throws Exception {
        AtomicReference<String> uploadRequest = new AtomicReference<>();
        AtomicReference<String> messageRequest = new AtomicReference<>();

        try (WhatsappHttpTestServer server = new WhatsappHttpTestServer(uploadRequest, messageRequest)) {
            VendaService vendaService = mock(VendaService.class);
            Venda venda = vendaComNfce(55L, "35260812345678000123650010000000011000000012");

            when(vendaService.buscarPorId(55L)).thenReturn(venda);
            when(vendaService.carregarPdfNfce(55L)).thenReturn("%PDF-1.4 TESTE".getBytes(StandardCharsets.UTF_8));

            WhatsappService service = buildService(server.baseUrl(), vendaService);
            var response = service.enviarNfce(55L, "+55 (11) 99999-0000", null);

            assertThat(response.vendaId()).isEqualTo(55L);
            assertThat(response.telefone()).isEqualTo("5511999990000");
            assertThat(response.mediaId()).isEqualTo("media-123");
            assertThat(response.messageId()).isEqualTo("wamid-456");
            assertThat(uploadRequest.get()).contains("name=\"messaging_product\"");
            assertThat(uploadRequest.get()).contains("filename=\"35260812345678000123650010000000011000000012.pdf\"");
            assertThat(uploadRequest.get()).contains("%PDF-1.4 TESTE");
            assertThat(messageRequest.get()).contains("\"to\":\"5511999990000\"");
            assertThat(messageRequest.get()).contains("\"id\":\"media-123\"");
        }
    }

    @Test
    void validaWebhookAssinadoQuandoAppSecretEstaConfigurado() throws Exception {
        VendaService vendaService = mock(VendaService.class);
        WhatsappIntegrationProperties properties = new WhatsappIntegrationProperties();
        properties.setVerifyToken("verify-token");
        properties.setAppSecret("super-secret");

        WhatsappService service = new WhatsappService(properties, vendaService);
        String payload = """
                {
                  "object": "whatsapp_business_account",
                  "entry": [
                    {
                      "changes": [
                        {
                          "value": {
                            "messages": [
                              { "id": "wamid-1" }
                            ],
                            "statuses": [
                              { "id": "wamid-1", "status": "sent" }
                            ]
                          }
                        }
                      ]
                    }
                  ]
                }
                """;

        String signature = "sha256=" + hmacSha256Hex("super-secret", payload);
        Map<String, Object> response = service.processarWebhook(signature, payload);

        assertThat(response.get("received")).isEqualTo(true);
        assertThat(response.get("messageCount")).isEqualTo(1);
        assertThat(response.get("statusCount")).isEqualTo(1);
    }

    @Test
    void rejeitaWebhookQuandoAssinaturaNaoConfere() {
        VendaService vendaService = mock(VendaService.class);
        WhatsappIntegrationProperties properties = new WhatsappIntegrationProperties();
        properties.setAppSecret("super-secret");

        WhatsappService service = new WhatsappService(properties, vendaService);

        assertThatThrownBy(() -> service.processarWebhook("sha256=00", "{}"))
                .isInstanceOf(AccessDeniedException.class);
    }

    private WhatsappService buildService(String baseUrl, VendaService vendaService) {
        WhatsappIntegrationProperties properties = new WhatsappIntegrationProperties();
        properties.setBaseUrl(baseUrl);
        properties.setApiVersion("v23.0");
        properties.setPhoneNumberId("123456789");
        properties.setAccessToken("token-teste");
        properties.setConnectTimeoutSeconds(2);
        properties.setReadTimeoutSeconds(2);
        return new WhatsappService(properties, vendaService);
    }

    private Venda vendaComNfce(Long id, String chaveAcesso) {
        Loja loja = new Loja();
        loja.setId(1L);
        loja.setNome("ISA Kids Centro");

        Venda venda = new Venda();
        venda.setId(id);
        venda.setLoja(loja);
        venda.setUsuario(new Usuario());
        venda.setStatus(StatusVenda.CONCLUIDA);
        venda.setFormaPagamento(FormaPagamento.PIX);
        venda.setTotalBruto(BigDecimal.TEN);
        venda.setTotalLiquido(BigDecimal.TEN);
        venda.setNotaFiscalChaveAcesso(chaveAcesso);
        return venda;
    }

    private String hmacSha256Hex(String secret, String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] bytes = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte current : bytes) {
            builder.append(String.format("%02x", current));
        }
        return builder.toString();
    }

    private static final class WhatsappHttpTestServer implements AutoCloseable {
        private final HttpServer server;
        private final AtomicReference<String> uploadRequest;
        private final AtomicReference<String> messageRequest;

        private WhatsappHttpTestServer(AtomicReference<String> uploadRequest, AtomicReference<String> messageRequest)
                throws IOException {
            this.uploadRequest = uploadRequest;
            this.messageRequest = messageRequest;
            this.server = HttpServer.create(new InetSocketAddress(0), 0);
            this.server.createContext("/v23.0/123456789/media", this::handleUpload);
            this.server.createContext("/v23.0/123456789/messages", this::handleMessage);
            this.server.start();
        }

        private void handleUpload(HttpExchange exchange) throws IOException {
            uploadRequest.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"id\":\"media-123\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        }

        private void handleMessage(HttpExchange exchange) throws IOException {
            messageRequest.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = """
                    {
                      "messages": [
                        { "id": "wamid-456" }
                      ]
                    }
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        }

        private String baseUrl() {
            return "http://localhost:" + server.getAddress().getPort();
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
