package isakids.isa.services;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import isakids.isa.config.FiscalIntegrationProperties;
import isakids.isa.dto.FiscalNfceIntegrationItemRequest;
import isakids.isa.dto.FiscalNfceIntegrationRequest;
import isakids.isa.dto.FiscalNfceResponse;
import isakids.isa.entities.ItemVenda;
import isakids.isa.entities.Produto;
import isakids.isa.entities.Venda;

@Service
public class FiscalIntegrationService {

    private final FiscalIntegrationProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public FiscalIntegrationService(FiscalIntegrationProperties properties) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(1, properties.getConnectTimeoutSeconds())))
                .build();
    }

    public FiscalNfceResponse emitirNfce(Venda venda, String cpfCnpjConsumidor) {
        try {
            BigDecimal desconto = normalizeMoney(venda.getDesconto());
            FiscalNfceIntegrationRequest request = new FiscalNfceIntegrationRequest(
                    venda.getLoja().getCnpj(),
                    venda.getId(),
                    null,
                    venda.getDataHora(),
                    desconto,
                    venda.getFormaPagamento(),
                    cpfCnpjConsumidor,
                    toItens(venda, desconto));

            String body = objectMapper.writeValueAsString(request);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(resolveUri("/api/integracoes/pdv/nfce"))
                    .timeout(Duration.ofSeconds(Math.max(1, properties.getReadTimeoutSeconds())))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));

            applyToken(builder);
            HttpResponse<String> response = httpClient.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            ensureSuccess(response.statusCode(), response.body(), "emitir NFC-e");
            return objectMapper.readValue(response.body(), FiscalNfceResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao integrar com o emissor fiscal: " + e.getMessage(), e);
        }
    }

    public byte[] baixarPdf(String chaveAcesso) {
        return baixarDocumento(chaveAcesso, "pdf");
    }

    public byte[] baixarXml(String chaveAcesso) {
        return baixarDocumento(chaveAcesso, "xml");
    }

    public byte[] baixarPdf(Venda venda) {
        return baixarDocumento(venda, "pdf");
    }

    public byte[] baixarXml(Venda venda) {
        return baixarDocumento(venda, "xml");
    }

    private byte[] baixarDocumento(String chaveAcesso, String extensao) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(resolveUri("/api/nfce/documentos/" + chaveAcesso + "/" + extensao))
                    .timeout(Duration.ofSeconds(Math.max(1, properties.getReadTimeoutSeconds())))
                    .GET();
            applyToken(builder);

            HttpResponse<byte[]> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
            ensureSuccess(response.statusCode(), new String(response.body(), StandardCharsets.UTF_8),
                    "baixar " + extensao.toUpperCase() + " da NFC-e");
            return response.body();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao baixar " + extensao.toUpperCase() + " da NFC-e: " + e.getMessage(),
                    e);
        }
    }

    private byte[] baixarDocumento(Venda venda, String extensao) {
        try {
            return baixarDocumentoPorVenda(venda, extensao);
        } catch (IllegalStateException e) {
            if (StringUtils.hasText(venda.getNotaFiscalChaveAcesso())) {
                return baixarDocumento(venda.getNotaFiscalChaveAcesso(), extensao);
            }
            throw e;
        }
    }

    private byte[] baixarDocumentoPorVenda(Venda venda, String extensao) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(resolveDocumentoUri(venda, extensao))
                    .timeout(Duration.ofSeconds(Math.max(1, properties.getReadTimeoutSeconds())))
                    .GET();
            applyToken(builder);

            HttpResponse<byte[]> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
            ensureSuccess(response.statusCode(), new String(response.body(), StandardCharsets.UTF_8),
                    "baixar " + extensao.toUpperCase() + " da NFC-e");
            return response.body();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao baixar " + extensao.toUpperCase() + " da NFC-e: " + e.getMessage(),
                    e);
        }
    }

    private List<FiscalNfceIntegrationItemRequest> toItens(Venda venda, BigDecimal descontoTotal) {
        List<ItemVenda> itens = venda.getItens();
        List<BigDecimal> descontosPorItem = ratearDesconto(itens, descontoTotal);
        List<FiscalNfceIntegrationItemRequest> result = new ArrayList<>(itens.size());
        for (int i = 0; i < itens.size(); i++) {
            result.add(toItem(itens.get(i), descontosPorItem.get(i)));
        }
        return result;
    }

    private FiscalNfceIntegrationItemRequest toItem(ItemVenda itemVenda, BigDecimal descontoItem) {
        Produto produto = itemVenda.getProduto();
        return new FiscalNfceIntegrationItemRequest(
                itemVenda.getId(),
                produto.getId(),
                produto.getDescricao(),
                produto.getCodigoBarras(),
                produto.getNcm(),
                produto.getCest(),
                produto.getOrigem(),
                produto.getCfop(),
                produto.getUnidadeComercial(),
                itemVenda.getQuantidade(),
                itemVenda.getPrecoUnitario(),
                itemVenda.getSubtotal(),
                descontoItem);
    }

    private List<BigDecimal> ratearDesconto(List<ItemVenda> itens, BigDecimal descontoTotal) {
        if (itens == null || itens.isEmpty()) {
            return List.of();
        }

        BigDecimal descontoNormalizado = normalizeMoney(descontoTotal);
        if (descontoNormalizado.signum() <= 0) {
            return itens.stream()
                    .map(item -> BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .toList();
        }

        List<Long> subtotaisEmCentavos = itens.stream()
                .map(ItemVenda::getSubtotal)
                .map(this::toCents)
                .toList();
        long totalBrutoEmCentavos = subtotaisEmCentavos.stream()
                .mapToLong(Long::longValue)
                .sum();
        long descontoEmCentavos = toCents(descontoNormalizado);

        if (totalBrutoEmCentavos <= 0 || descontoEmCentavos <= 0) {
            return itens.stream()
                    .map(item -> BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .toList();
        }

        List<Long> descontosBase = new ArrayList<>(itens.size());
        List<RateioRemainder> remainders = new ArrayList<>(itens.size());
        long alocado = 0;

        for (int i = 0; i < subtotaisEmCentavos.size(); i++) {
            long subtotalEmCentavos = subtotaisEmCentavos.get(i);
            long numerador = descontoEmCentavos * subtotalEmCentavos;
            long descontoBase = numerador / totalBrutoEmCentavos;
            long resto = numerador % totalBrutoEmCentavos;
            descontosBase.add(descontoBase);
            remainders.add(new RateioRemainder(i, resto));
            alocado += descontoBase;
        }

        long restante = descontoEmCentavos - alocado;
        remainders.sort((left, right) -> Long.compare(right.remainder(), left.remainder()));
        for (int i = 0; i < restante; i++) {
            int index = remainders.get(i).index();
            descontosBase.set(index, descontosBase.get(index) + 1);
        }

        return descontosBase.stream()
                .map(this::fromCents)
                .toList();
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : value.setScale(2, RoundingMode.HALF_UP);
    }

    private long toCents(BigDecimal value) {
        return normalizeMoney(value)
                .movePointRight(2)
                .longValueExact();
    }

    private BigDecimal fromCents(long value) {
        return BigDecimal.valueOf(value, 2).setScale(2, RoundingMode.HALF_UP);
    }

    private record RateioRemainder(int index, long remainder) {
    }

    private void applyToken(HttpRequest.Builder builder) {
        if (StringUtils.hasText(properties.getToken())) {
            builder.header("X-Integration-Token", properties.getToken().trim());
        }
    }

    private URI resolveDocumentoUri(Venda venda, String extensao) {
        if (venda == null || venda.getId() == null) {
            throw new IllegalStateException("Venda invalida para baixar documento fiscal.");
        }
        if (venda.getLoja() == null || !StringUtils.hasText(venda.getLoja().getCnpj())) {
            throw new IllegalStateException("Venda sem loja/CNPJ configurado para baixar documento fiscal.");
        }

        String lojaCnpj = normalizeDigits(venda.getLoja().getCnpj(), "loja.cnpj");
        String encodedCnpj = URLEncoder.encode(lojaCnpj, StandardCharsets.UTF_8);
        return resolveUri("/api/integracoes/pdv/nfce/vendas/" + venda.getId() + "/" + extensao + "?lojaCnpj=" + encodedCnpj);
    }

    private URI resolveUri(String path) {
        String baseUrl = properties.getBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalStateException("app.integracoes.fiscal.base-url nao foi configurada.");
        }
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return URI.create(normalizedBaseUrl + path);
    }

    private void ensureSuccess(int statusCode, String body, String action) {
        if (statusCode >= 200 && statusCode < 300) {
            return;
        }
        String detail = StringUtils.hasText(body) ? body : "sem corpo de resposta";
        throw new IllegalStateException("Falha ao " + action + " no emissor fiscal. HTTP " + statusCode + ": " + detail);
    }

    private String normalizeDigits(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(fieldName + " nao foi configurado.");
        }
        return value.replaceAll("\\D", "");
    }
}
