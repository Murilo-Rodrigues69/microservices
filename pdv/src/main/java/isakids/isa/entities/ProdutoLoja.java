package isakids.isa.entities;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "produtos_loja",
        uniqueConstraints = @UniqueConstraint(columnNames = {"produto_id", "loja_id"})
)
public class ProdutoLoja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "loja_id", nullable = false)
    private Loja loja;

    @Column(name = "quantidade_estoque", nullable = false)
    private Integer quantidadeEstoque = 0;

    public ProdutoLoja() {
    }

    public ProdutoLoja(Produto produto, Loja loja, Integer quantidadeEstoque) {
        this.produto = produto;
        this.loja = loja;
        this.quantidadeEstoque = quantidadeEstoque == null ? 0 : quantidadeEstoque;
    }

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

    public Integer getQuantidadeEstoque() {
        return quantidadeEstoque;
    }

    public void setQuantidadeEstoque(Integer quantidadeEstoque) {
        this.quantidadeEstoque = quantidadeEstoque == null ? 0 : quantidadeEstoque;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, loja, produto, quantidadeEstoque);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ProdutoLoja other = (ProdutoLoja) obj;
        return Objects.equals(id, other.id) && Objects.equals(loja, other.loja)
                && Objects.equals(produto, other.produto)
                && Objects.equals(quantidadeEstoque, other.quantidadeEstoque);
    }
}
