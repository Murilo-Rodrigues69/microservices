package isakids.isa.dto;

import java.math.BigDecimal;

public class EstoqueResumoDTO {

    Long totalItens;
    Long quantidadeProdutosEstoqueBaixo;
    BigDecimal valorTotalEstoque;

    public EstoqueResumoDTO() {

    }

    public EstoqueResumoDTO(Long totalItens, Long quantidadeProdutosEstoqueBaixo, BigDecimal valorTotalEstoque) {
        super();
        this.totalItens = totalItens;
        this.quantidadeProdutosEstoqueBaixo = quantidadeProdutosEstoqueBaixo;
        this.valorTotalEstoque = valorTotalEstoque;
    }

    public Long getTotalItens() {
        return totalItens;
    }

    public void setTotalItens(Long totalItens) {
        this.totalItens = totalItens;
    }

    public Long getQuantidadeProdutosEstoqueBaixo() {
        return quantidadeProdutosEstoqueBaixo;
    }

    public void setQuantidadeProdutosEstoqueBaixo(Long quantidadeProdutosEstoqueBaixo) {
        this.quantidadeProdutosEstoqueBaixo = quantidadeProdutosEstoqueBaixo;
    }

    public BigDecimal getValorTotalEstoque() {
        return valorTotalEstoque;
    }

    public void setValorTotalEstoque(BigDecimal valorTotalEstoque) {
        this.valorTotalEstoque = valorTotalEstoque;
    }


}
