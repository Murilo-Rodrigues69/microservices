package isakids.nfcelab.dto;

import java.math.BigDecimal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public class NfceItemRequest {

    private Long itemId;
    private Long produtoEmpresaId;

    @NotNull
    @Positive
    private Integer quantidade;

    @NotNull
    @Positive
    private BigDecimal precoUnitario;

    @PositiveOrZero
    private BigDecimal subtotal;

    @PositiveOrZero
    private BigDecimal desconto;

    @Valid
    private NfceProductRequest produto;

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Long getProdutoEmpresaId() {
        return produtoEmpresaId;
    }

    public void setProdutoEmpresaId(Long produtoEmpresaId) {
        this.produtoEmpresaId = produtoEmpresaId;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(BigDecimal precoUnitario) {
        this.precoUnitario = precoUnitario;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getDesconto() {
        return desconto;
    }

    public void setDesconto(BigDecimal desconto) {
        this.desconto = desconto;
    }

    public NfceProductRequest getProduto() {
        return produto;
    }

    public void setProduto(NfceProductRequest produto) {
        this.produto = produto;
    }
}
