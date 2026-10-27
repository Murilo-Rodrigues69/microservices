package isakids.isa.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "produtos")
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigoInterno;

    @Column(nullable = false, unique = true)
    private String codigoBarras;


    private String descricao;
    private String categoria;
    private String tamanho;
    private String cor;

    @Column(length = 8)
    private String ncm;

    @Column(length = 7)
    private String cest;

    @Column(length = 120)
    private String origem;

    @Column(length = 4)
    private String cfop;

    @Column(name = "unidade_comercial", length = 10)
    private String unidadeComercial;

    @Column(name = "preco_custo", precision = 15, scale = 2)
    private BigDecimal precoCusto;

    @Column(name = "preco_venda", precision = 15, scale = 2)
    private BigDecimal precoVenda;

    @Column(nullable = false)
    private Boolean ativo = true;


    LocalDateTime dataCadastro;
    LocalDateTime dataAtualizado;

    @PrePersist
    public void beforeInsert() {
        this.dataCadastro = LocalDateTime.now();
        this.dataAtualizado = LocalDateTime.now();
    }

    @PreUpdate
    public void beforeUpdate() {
        this.dataAtualizado = LocalDateTime.now();
    }


    public Produto() {

    }


    public Produto(Long id, String codigoInterno, String codigoBarras, String descricao, String categoria,
            String tamanho, String cor, String ncm, String cest, String origem, String cfop, String unidadeComercial,
            BigDecimal precoCusto, BigDecimal precoVenda, Boolean ativo, LocalDateTime dataCadastro,
            LocalDateTime dataAtualizado) {
        super();
        this.id = id;
        this.codigoInterno = codigoInterno;
        this.codigoBarras = codigoBarras;
        this.descricao = descricao;
        this.categoria = categoria;
        this.tamanho = tamanho;
        this.cor = cor;
        this.ncm = ncm;
        this.cest = cest;
        this.origem = origem;
        this.cfop = cfop;
        this.unidadeComercial = unidadeComercial;
        this.precoCusto = precoCusto;
        this.precoVenda = precoVenda;
        this.ativo = ativo;
        this.dataCadastro = dataCadastro;
        this.dataAtualizado = dataAtualizado;
    }




    public Long getId() {
        return id;
    }


    public void setId(Long id) {
        this.id = id;
    }


    public String getCodigoInterno() {
        return codigoInterno;
    }


    public void setCodigoInterno(String codigoInterno) {
        this.codigoInterno = codigoInterno;
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

    public String getNcm() {
        return ncm;
    }

    public void setNcm(String ncm) {
        this.ncm = ncm;
    }

    public String getCest() {
        return cest;
    }

    public void setCest(String cest) {
        this.cest = cest;
    }

    public String getOrigem() {
        return origem;
    }

    public void setOrigem(String origem) {
        this.origem = origem;
    }

    public String getCfop() {
        return cfop;
    }

    public void setCfop(String cfop) {
        this.cfop = cfop;
    }

    public String getUnidadeComercial() {
        return unidadeComercial;
    }

    public void setUnidadeComercial(String unidadeComercial) {
        this.unidadeComercial = unidadeComercial;
    }


    public BigDecimal getPrecoCusto() {
        return precoCusto;
    }


    public void setPrecoCusto(BigDecimal precoCusto) {
        this.precoCusto = precoCusto;
    }


    public BigDecimal getPrecoVenda() {
        return precoVenda;
    }


    public void setPrecoVenda(BigDecimal precoVenda) {
        this.precoVenda = precoVenda;
    }


    public Boolean getAtivo() {
        return ativo;
    }


    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }


    public LocalDateTime getDataCadastro() {
        return dataCadastro;
    }


    public void setDataCadastro(LocalDateTime dataCadastro) {
        this.dataCadastro = dataCadastro;
    }


    public LocalDateTime getDataAtualizado() {
        return dataAtualizado;
    }


    public void setDataAtualizado(LocalDateTime dataAtualizado) {
        this.dataAtualizado = dataAtualizado;
    }


    @Override
    public int hashCode() {
        return Objects.hash(ativo, categoria, cest, cfop, codigoBarras, codigoInterno, cor, dataAtualizado,
                dataCadastro, descricao, id, ncm, origem, precoCusto, precoVenda, tamanho, unidadeComercial);
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Produto other = (Produto) obj;
        return Objects.equals(ativo, other.ativo) && Objects.equals(categoria, other.categoria)
                && Objects.equals(cest, other.cest) && Objects.equals(cfop, other.cfop)
                && Objects.equals(codigoBarras, other.codigoBarras)
                && Objects.equals(codigoInterno, other.codigoInterno) && Objects.equals(cor, other.cor)
                && Objects.equals(dataAtualizado, other.dataAtualizado)
                && Objects.equals(dataCadastro, other.dataCadastro) && Objects.equals(descricao, other.descricao)
                && Objects.equals(id, other.id) && Objects.equals(ncm, other.ncm)
                && Objects.equals(origem, other.origem) && Objects.equals(precoCusto, other.precoCusto)
                && Objects.equals(precoVenda, other.precoVenda) && Objects.equals(tamanho, other.tamanho)
                && Objects.equals(unidadeComercial, other.unidadeComercial);
    }


    @Override
    public String toString() {
        return "Produto [id=" + id + ", codigoInterno=" + codigoInterno + ", codigoBarras=" + codigoBarras
                + ", descricao=" + descricao + ", categoria=" + categoria + ", tamanho=" + tamanho + ", cor=" + cor
                + ", ncm=" + ncm + ", cest=" + cest + ", origem=" + origem + ", cfop=" + cfop
                + ", unidadeComercial=" + unidadeComercial + ", precoCusto=" + precoCusto + ", precoVenda="
                + precoVenda + ", ativo=" + ativo + ", dataCadastro=" + dataCadastro + ", dataAtualizado="
                + dataAtualizado + "]";
    }



}
