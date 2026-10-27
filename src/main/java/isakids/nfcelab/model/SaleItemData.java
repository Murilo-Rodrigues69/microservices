package isakids.nfcelab.model;

import java.math.BigDecimal;

public record SaleItemData(
        Long id,
        Integer quantidade,
        BigDecimal precoUnitario,
        BigDecimal subtotal,
        BigDecimal desconto,
        ProductData produto) {

    public SaleItemData(
            Long id,
            Integer quantidade,
            BigDecimal precoUnitario,
            BigDecimal subtotal,
            ProductData produto) {
        this(id, quantidade, precoUnitario, subtotal, BigDecimal.ZERO, produto);
    }

    public BigDecimal subtotalOrCalculated() {
        if (subtotal != null) {
            return subtotal;
        }
        if (precoUnitario == null || quantidade == null) {
            return BigDecimal.ZERO;
        }
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public BigDecimal descontoOrZero() {
        return desconto != null ? desconto : BigDecimal.ZERO;
    }
}
