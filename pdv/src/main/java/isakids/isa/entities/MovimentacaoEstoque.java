package isakids.isa.entities;


import java.time.LocalDateTime;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

import isakids.isa.enums.TipoMovimentoEstoque;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "movimentacoes_estoque")
public class MovimentacaoEstoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    @JsonIgnore
    private Produto produto;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "loja_id", nullable = false)
    private Loja loja;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimentoEstoque tipo;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    // Quem fez a movimentação (entrada, ajuste, etc.)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    // Opcional: linkar com a venda que gerou a saída
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venda_id")
    @JsonIgnore
    private Venda venda;

    @Column(length = 255)
    private String observacao;

    // ====== CONSTRUTORES ======

    public MovimentacaoEstoque() {
    }

    public MovimentacaoEstoque(Produto produto,
                               Loja loja,
                               TipoMovimentoEstoque tipo,
                               Integer quantidade,
                               Usuario usuario,
                               Venda venda,
                               String observacao) {
        this.produto = produto;
        this.loja = loja;
        this.tipo = tipo;
        this.quantidade = quantidade;
        this.usuario = usuario;
        this.venda = venda;
        this.observacao = observacao;
        this.dataHora = LocalDateTime.now();
    }

    // ====== GETTERS / SETTERS ======

    public Long getId() {
        return id;
    }

    public Produto getProduto() {
        return produto;
    }

    public void setProduto(Produto produto) {
        this.produto = produto;
    }

    public Loja getLoja() {
        return loja;
    }

    public void setLoja(Loja loja) {
        this.loja = loja;
    }

    public TipoMovimentoEstoque getTipo() {
        return tipo;
    }

    public void setTipo(TipoMovimentoEstoque tipo) {
        this.tipo = tipo;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Venda getVenda() {
        return venda;
    }

    public void setVenda(Venda venda) {
        this.venda = venda;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataHora, id, loja, observacao, produto, quantidade, tipo, usuario, venda);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MovimentacaoEstoque other = (MovimentacaoEstoque) obj;
        return Objects.equals(dataHora, other.dataHora) && Objects.equals(id, other.id)
                && Objects.equals(loja, other.loja) && Objects.equals(observacao, other.observacao)
                && Objects.equals(produto, other.produto)
                && Objects.equals(quantidade, other.quantidade) && tipo == other.tipo
                && Objects.equals(usuario, other.usuario) && Objects.equals(venda, other.venda);
    }

    @Override
    public String toString() {
        return "MovimentacaoEstoque [id=" + id + ", produto=" + produto + ", loja=" + loja + ", tipo=" + tipo
                + ", quantidade=" + quantidade + ", dataHora=" + dataHora + ", usuario=" + usuario + ", venda="
                + venda + ", observacao=" + observacao + "]";
    }


}
