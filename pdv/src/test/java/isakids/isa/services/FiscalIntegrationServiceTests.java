package isakids.isa.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import isakids.isa.config.FiscalIntegrationProperties;
import isakids.isa.entities.ItemVenda;
import isakids.isa.entities.Loja;
import isakids.isa.entities.Produto;
import isakids.isa.entities.Usuario;
import isakids.isa.entities.Venda;
import isakids.isa.enums.FormaPagamento;
import isakids.isa.enums.StatusVenda;

class FiscalIntegrationServiceTests {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void enviaDescontoIntegralNoItemQuandoVendaTemUmUnicoProduto() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        try (HttpTestServer server = new HttpTestServer(requestBody, new AtomicReference<>())) {
            FiscalIntegrationService service = buildService(server.baseUrl());

            Venda venda = vendaComItens(List.of(
                    item("Camiseta", new BigDecimal("79.90"), 1)));
            venda.setDesconto(new BigDecimal("0.90"));

            service.emitirNfce(venda, null);

            JsonNode payload = objectMapper.readTree(requestBody.get());
            assertThat(payload.path("desconto").decimalValue()).isEqualByComparingTo("0.90");
            assertThat(payload.path("itens")).hasSize(1);
            assertThat(payload.path("itens").get(0).path("desconto").decimalValue()).isEqualByComparingTo("0.90");
        }
    }

    @Test
    void rateiaDescontoEntreItensMantendoSomatorioExato() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        try (HttpTestServer server = new HttpTestServer(requestBody, new AtomicReference<>())) {
            FiscalIntegrationService service = buildService(server.baseUrl());

            Venda venda = vendaComItens(List.of(
                    item("Produto A", new BigDecimal("33.33"), 1),
                    item("Produto B", new BigDecimal("33.33"), 1),
                    item("Produto C", new BigDecimal("33.34"), 1)));
            venda.setDesconto(new BigDecimal("0.01"));

            service.emitirNfce(venda, null);

            JsonNode itens = objectMapper.readTree(requestBody.get()).path("itens");
            BigDecimal descontoRateado = BigDecimal.ZERO;
            for (JsonNode item : itens) {
                BigDecimal descontoItem = item.path("desconto").decimalValue();
                assertThat(descontoItem).isGreaterThanOrEqualTo(BigDecimal.ZERO);
                descontoRateado = descontoRateado.add(descontoItem);
            }

            assertThat(descontoRateado).isEqualByComparingTo("0.01");
        }
    }

    @Test
    void baixaPdfDaNfcePelaVendaELoja() throws Exception {
        AtomicReference<String> requestPath = new AtomicReference<>();
        try (HttpTestServer server = new HttpTestServer(new AtomicReference<>(), requestPath)) {
            FiscalIntegrationService service = buildService(server.baseUrl());
            Venda venda = vendaComItens(List.of(item("Camiseta", new BigDecimal("79.90"), 1)));
            venda.setId(321L);

            byte[] pdf = service.baixarPdf(venda);

            assertThat(pdf).isEqualTo("%PDF-TESTE".getBytes(StandardCharsets.UTF_8));
            assertThat(requestPath.get()).isEqualTo("/api/integracoes/pdv/nfce/vendas/321/pdf?lojaCnpj=11222333000144");
        }
    }

    private FiscalIntegrationService buildService(String baseUrl) {
        FiscalIntegrationProperties properties = new FiscalIntegrationProperties();
        properties.setBaseUrl(baseUrl);
        properties.setConnectTimeoutSeconds(2);
        properties.setReadTimeoutSeconds(2);
        return new FiscalIntegrationService(properties);
    }

    private Venda vendaComItens(List<ItemVenda> itens) {
        Loja loja = new Loja();
        loja.setId(10L);
        loja.setCnpj("11222333000144");
        loja.setNome("Loja Centro");

        Venda venda = new Venda();
        venda.setId(1L);
        venda.setLoja(loja);
        venda.setUsuario(new Usuario());
        venda.setStatus(StatusVenda.CONCLUIDA);
        venda.setFormaPagamento(FormaPagamento.PIX);
        venda.setItens(itens);
        for (ItemVenda item : itens) {
            item.setVenda(venda);
        }
        venda.recalcularTotais();
        return venda;
    }

    private ItemVenda item(String descricao, BigDecimal precoUnitario, int quantidade) {
        Produto produto = new Produto();
        produto.setId((long) descricao.hashCode());
        produto.setDescricao(descricao);
        produto.setCodigoBarras("SEM GTIN");
        produto.setNcm("61091000");
        produto.setOrigem("NACIONAL");
        produto.setCfop("5102");
        produto.setUnidadeComercial("UN");

        return new ItemVenda(null, produto, quantidade, precoUnitario);
    }

    private static final class HttpTestServer implements AutoCloseable {
        private final HttpServer server;
        private final AtomicReference<String> requestBody;
        private final AtomicReference<String> requestPath;

        private HttpTestServer(AtomicReference<String> requestBody, AtomicReference<String> requestPath) throws IOException {
            this.requestBody = requestBody;
            this.requestPath = requestPath;
            this.server = HttpServer.create(new InetSocketAddress(0), 0);
            this.server.createContext("/api/integracoes/pdv/nfce", this::handleNfceRequest);
            this.server.createContext("/api/integracoes/pdv/nfce/vendas", this::handleDocumentoRequest);
            this.server.start();
        }

        private void handleNfceRequest(HttpExchange exchange) throws IOException {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        }

        private void handleDocumentoRequest(HttpExchange exchange) throws IOException {
            requestPath.set(exchange.getRequestURI().toString());
            byte[] response = "%PDF-TESTE".getBytes(StandardCharsets.UTF_8);
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
