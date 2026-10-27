package isakids.nfcelab.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import isakids.nfcelab.config.NfceDocumentProperties;
import isakids.nfcelab.support.SampleAuthorizedXmls;
import tools.jackson.databind.ObjectMapper;

class MeuDanfeClientTests {

    @Test
    void enviaXmlEmTextPlainERecebePdfEmBase64() throws Exception {
        AtomicReference<String> receivedApiKey = new AtomicReference<>();
        AtomicReference<String> receivedContentType = new AtomicReference<>();
        AtomicReference<String> receivedBody = new AtomicReference<>();

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v2/fd/convert/xml-to-da", new SuccessHandler(receivedApiKey, receivedContentType, receivedBody));
        server.start();
        try {
            NfceDocumentProperties properties = new NfceDocumentProperties();
            properties.getMeuDanfe().setBaseUrl("http://localhost:" + server.getAddress().getPort() + "/v2");
            properties.getMeuDanfe().setApiKey("teste-api-key");

            MeuDanfeClient client = new MeuDanfeClient(properties, new ObjectMapper());

            byte[] pdf = client.gerarDanfePdf(SampleAuthorizedXmls.authorizedNfe());

            assertThat(receivedApiKey.get()).isEqualTo("teste-api-key");
            assertThat(receivedContentType.get()).contains("text/plain");
            assertThat(receivedBody.get()).contains("<nfeProc");
            assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void baixaXmlEPdfPorChave() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v2/fd/add/52260411222333000144550010000020011573727506",
                exchange -> respondJson(exchange, """
                        {
                          "value": "52260411222333000144550010000020011573727506",
                          "type": "NFE",
                          "status": "OK",
                          "statusMessage": "Ok"
                        }
                        """));
        server.createContext("/v2/fd/get/xml/52260411222333000144550010000020011573727506",
                exchange -> respondJson(exchange, """
                        {
                          "name": "NFE-52260411222333000144550010000020011573727506.xml",
                          "type": "NFE",
                          "format": "XML",
                          "data": %s
                        }
                        """.formatted(jsonString(SampleAuthorizedXmls.authorizedNfe()))));
        server.createContext("/v2/fd/get/da/52260411222333000144550010000020011573727506",
                exchange -> respondJson(exchange, """
                        {
                          "name": "NFE-52260411222333000144550010000020011573727506.pdf",
                          "type": "NFE",
                          "format": "BASE64",
                          "data": "%s"
                        }
                        """.formatted(Base64.getEncoder().encodeToString(SampleAuthorizedXmls.minimalPdfBytes()))));
        server.start();
        try {
            NfceDocumentProperties properties = new NfceDocumentProperties();
            properties.getMeuDanfe().setBaseUrl("http://localhost:" + server.getAddress().getPort() + "/v2");
            properties.getMeuDanfe().setApiKey("teste-api-key");

            MeuDanfeClient client = new MeuDanfeClient(properties, new ObjectMapper());

            MeuDanfeClient.MeuDanfeFetchedDocument document = client
                    .baixarDocumentosPorChave("52260411222333000144550010000020011573727506")
                    .orElseThrow();

            assertThat(document.xml()).contains("<nfeProc");
            assertThat(new String(document.pdf(), 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        } finally {
            server.stop(0);
        }
    }

    private static void respondJson(HttpExchange exchange, String body) throws IOException {
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, payload.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(payload);
        }
    }

    private static String jsonString(String value) {
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n") + "\"";
    }

    private static class SuccessHandler implements HttpHandler {
        private final AtomicReference<String> receivedApiKey;
        private final AtomicReference<String> receivedContentType;
        private final AtomicReference<String> receivedBody;

        private SuccessHandler(AtomicReference<String> receivedApiKey, AtomicReference<String> receivedContentType,
                AtomicReference<String> receivedBody) {
            this.receivedApiKey = receivedApiKey;
            this.receivedContentType = receivedContentType;
            this.receivedBody = receivedBody;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            receivedApiKey.set(exchange.getRequestHeaders().getFirst("Api-Key"));
            receivedContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));

            String pdfBase64 = Base64.getEncoder().encodeToString(SampleAuthorizedXmls.minimalPdfBytes());
            String body = """
                    {
                      "name": "NFE-TESTE.pdf",
                      "type": "NFE",
                      "format": "BASE64",
                      "data": "%s"
                    }
                    """.formatted(pdfBase64);

            byte[] payload = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, payload.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(payload);
            }
        }
    }
}
