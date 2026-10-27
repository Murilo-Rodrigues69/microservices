package isakids.isa.dto;

import java.math.BigDecimal;

import isakids.isa.entities.Produto;
import isakids.isa.entities.ProdutoLoja;

public class ProdutoBuscaResponse {

    private Long produtoLojaId;
    private Long produtoId;
    private Long lojaId;
    private String lojaNome;
    private String codigoBarras;
    private String descricao;
    private String categoria;
    private String tamanho;
    private String cor;
    private BigDecimal precoVenda;
    private Integer quantidadeEstoque;

    public static ProdutoBuscaResponse fromEntity(ProdutoLoja produtoLoja) {
        ProdutoBuscaResponse response = new ProdutoBuscaResponse();
        Produto produto = produtoLoja.getProduto();

        response.setProdutoLojaId(produtoLoja.getId());
        response.setQuantidadeEstoque(produtoLoja.getQuantidadeEstoque());

        if (produtoLoja.getLoja() != null) {
            response.setLojaId(produtoLoja.getLoja().getId());
            response.setLojaNome(produtoLoja.getLoja().getNome());
        }

        if (produto != null) {
            response.setProdutoId(produto.getId());
            response.setCodigoBarras(produto.getCodigoBarras());
            response.setDescricao(produto.getDescricao());
            response.setCategoria(produto.getCategoria());
            response.setTamanho(produto.getTamanho());
            response.setCor(produto.getCor());
            response.setPrecoVenda(produto.getPrecoVenda());
        }

        return response;
    }

    public Long getProdutoLojaId() {
        return produtoLojaId;
    }

    public void setProdutoLojaId(Long produtoLojaId) {
        this.produtoLojaId = produtoLojaId;
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

    public String getLojaNome() {
        return lojaNome;
    }

    public void setLojaNome(String lojaNome) {
        this.lojaNome = lojaNome;
    }

    public String getCodigoBarras() {
        return codigoBarras;
    }

    public void setCodigoBarras(String codigoBarras) {
        this.codigoBarras = codigoBarras;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getTamanho() {
        return tamanho;
    }

    public void setTamanho(String tamanho) {
        this.tamanho = tamanho;
    }

    public String getCor() {
        return cor;
    }

    public void setCor(String cor) {
        this.cor = cor;
    }

    public BigDecimal getPrecoVenda() {
        return precoVenda;
    }

    public void setPrecoVenda(BigDecimal precoVenda) {
        this.precoVenda = precoVenda;
    }

    public Integer getQuantidadeEstoque() {
        return quantidadeEstoque;
    }

    public void setQuantidadeEstoque(Integer quantidadeEstoque) {
        this.quantidadeEstoque = quantidadeEstoque;
    }
}
