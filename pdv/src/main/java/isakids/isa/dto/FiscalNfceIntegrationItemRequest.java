package isakids.isa.dto;

import java.math.BigDecimal;

public record FiscalNfceIntegrationItemRequest(
        Long itemId,
        Long produtoId,
        String descricao,
        String codigoBarras,
        String ncm,
        String cest,
        String origem,
        String cfop,
        String unidadeComercial,
        Integer quantidade,
        BigDecimal precoUnitario,
        BigDecimal subtotal,
        BigDecimal desconto) {
}
