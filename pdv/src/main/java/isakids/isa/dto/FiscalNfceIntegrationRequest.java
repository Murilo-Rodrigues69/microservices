package isakids.isa.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import isakids.isa.enums.FormaPagamento;

public record FiscalNfceIntegrationRequest(
        String lojaCnpj,
        Long vendaId,
        String numeroNota,
        LocalDateTime dataHora,
        BigDecimal desconto,
        FormaPagamento formaPagamento,
        String cpfCnpjConsumidor,
        List<FiscalNfceIntegrationItemRequest> itens) {
}
