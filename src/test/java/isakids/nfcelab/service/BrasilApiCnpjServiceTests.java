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

import isakids.nfcelab.config.BrasilApiProperties;
import isakids.nfcelab.dto.EmpresaCnpjLookupResponse;
import isakids.nfcelab.exception.ResourceNotFoundException;
import tools.jackson.databind.ObjectMapper;

class BrasilApiCnpjServiceTests {

    @Test
    void consultaCnpjEMapeiaCamposUteisParaCadastro() throws Exception {
        AtomicReference<String> requestedPath = new AtomicReference<>();

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/cnpj/v1/27865757000102", exchange -> {
            requestedPath.set(exchange.getRequestURI().getPath());
            respondJson(exchange, """
                    {
                      "cnpj": "27865757000102",
                      "razao_social": "GLOBO COMUNICACAO E PARTICIPACOES S/A",
                      "nome_fantasia": "TV GLOBO",
                      "situacao_cadastral": 2,
                      "descricao_situacao_cadastral": "ATIVA",
                      "data_inicio_atividade": "1986-01-31",
                      "natureza_juridica": "Sociedade Anônima Fechada",
                      "porte": "DEMAIS",
                      "capital_social": 6983568400,
                      "email": "contato@empresa.com",
                      "ddd_telefone_1": "2121554551",
                      "ddd_telefone_2": "2121554552",
                      "cep": "22460901",
                      "descricao_tipo_de_logradouro": "RUA",
                      "logradouro": "LOPES QUINTAS",
                      "numero": "303",
                      "complemento": "",
                      "bairro": "JARDIM BOTANICO",
                      "municipio": "RIO DE JANEIRO",
                      "codigo_municipio_ibge": 3304557,
                      "uf": "RJ"
                    }
                    """);
        });
        server.start();
        try {
            BrasilApiProperties properties = new BrasilApiProperties();
            properties.setBaseUrl("http://localhost:" + server.getAddress().getPort() + "/api");

            BrasilApiCnpjService service = new BrasilApiCnpjService(properties, new ObjectMapper(), java.net.http.HttpClient.newHttpClient());

            EmpresaCnpjLookupResponse response = service.consultarPorCnpj("27.865.757/0001-02");

            assertThat(requestedPath.get()).isEqualTo("/api/cnpj/v1/27865757000102");
            assertThat(response.cnpj()).isEqualTo("27865757000102");
            assertThat(response.razaoSocial()).isEqualTo("GLOBO COMUNICACAO E PARTICIPACOES S/A");
            assertThat(response.nomeFantasia()).isEqualTo("TV GLOBO");
            assertThat(response.descricaoSituacaoCadastral()).isEqualTo("ATIVA");
            assertThat(response.logradouroCompleto()).isEqualTo("RUA LOPES QUINTAS");
            assertThat(response.codigoMunicipioIbge()).isEqualTo("3304557");
            assertThat(response.uf()).isEqualTo("RJ");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void retornaNotFoundQuandoBrasilApiResponder404() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/cnpj/v1/00000000000000", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.start();
        try {
            BrasilApiProperties properties = new BrasilApiProperties();
            properties.setBaseUrl("http://localhost:" + server.getAddress().getPort() + "/api");

            BrasilApiCnpjService service = new BrasilApiCnpjService(properties, new ObjectMapper(), java.net.http.HttpClient.newHttpClient());

            assertThatThrownBy(() -> service.consultarPorCnpj("00000000000000"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("CNPJ nao encontrado");
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
