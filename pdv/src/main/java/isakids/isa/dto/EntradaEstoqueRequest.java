package isakids.isa.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class EntradaEstoqueRequest {

    @NotNull
    private Long produtoId;
    private Long lojaId;
    @NotNull
    @Positive
    private Integer quantidade;
    private String observacao;

    public EntradaEstoqueRequest(){

    }

    public EntradaEstoqueRequest(Long produtoId, Long lojaId, Integer quantidade, String observacao) {
        super();
        this.produtoId = produtoId;
        this.lojaId = lojaId;
        this.quantidade = quantidade;
        this.observacao = observacao;
    }

    public Long getProdutoId() {
        return produtoId;
    }

    public void setProdutoId(Long produtoId) {
        this.produtoId = produtoId;
    }

    public Long getLojaId() {
        return lojaId;
    }

    public void setLojaId(Long lojaId) {
        this.lojaId = lojaId;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }


}
