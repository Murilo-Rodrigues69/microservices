package isakids.nfcelab.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import isakids.nfcelab.config.ViaCepProperties;
import isakids.nfcelab.dto.EnderecoCepLookupResponse;
import isakids.nfcelab.exception.ResourceNotFoundException;
import tools.jackson.databind.ObjectMapper;

class ViaCepServiceTests {

    @Test
    void consultaCepEMapeiaEnderecoParaOFrontend() throws Exception {
        AtomicReference<String> requestedPath = new AtomicReference<>();

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/ws/71827615/json/", exchange -> {
            requestedPath.set(exchange.getRequestURI().getPath());
            respondJson(exchange, """
                    {
                      "cep": "71827-615",
                      "logradouro": "Quadra QSA 10",
                      "complemento": "Lote 01",
                      "bairro": "Taguatinga Sul (Taguatinga)",
                      "localidade": "Brasília",
                      "uf": "DF",
                      "estado": "Distrito Federal",
                      "regiao": "Centro-Oeste",
                      "ibge": "5300108",
                      "gia": "",
                      "ddd": "61",
                      "siafi": "9701"
                    }
                    """);
        });
        server.start();
        try {
            ViaCepProperties properties = new ViaCepProperties();
            properties.setBaseUrl("http://localhost:" + server.getAddress().getPort() + "/ws");

            ViaCepService service = new ViaCepService(properties, new ObjectMapper(), java.net.http.HttpClient.newHttpClient());

            EnderecoCepLookupResponse response = service.consultarPorCep("71827-615");

            assertThat(requestedPath.get()).isEqualTo("/ws/71827615/json/");
            assertThat(response.cep()).isEqualTo("71827-615");
            assertThat(response.logradouro()).isEqualTo("Quadra QSA 10");
            assertThat(response.complemento()).isEqualTo("Lote 01");
            assertThat(response.localidade()).isEqualTo("Brasília");
            assertThat(response.uf()).isEqualTo("DF");
            assertThat(response.ibge()).isEqualTo("5300108");
            assertThat(response.ddd()).isEqualTo("61");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void retornaNotFoundQuandoViaCepResponderErroTrue() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/ws/00000000/json/", exchange -> respondJson(exchange, """
                {
                  "erro": true
                }
                """));
        server.start();
        try {
            ViaCepProperties properties = new ViaCepProperties();
            properties.setBaseUrl("http://localhost:" + server.getAddress().getPort() + "/ws");

            ViaCepService service = new ViaCepService(properties, new ObjectMapper(), java.net.http.HttpClient.newHttpClient());

            assertThatThrownBy(() -> service.consultarPorCep("00000000"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("CEP nao encontrado");
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
}
