package isakids.isa.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "itens_venda")
public class ItemVenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "venda_id", nullable = false)
    @JsonIgnore
    private Venda venda;


    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(name = "preco_unitario", nullable = false, precision = 15, scale = 2)
    private BigDecimal precoUnitario;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal;



    public ItemVenda() {
    }

    public ItemVenda(Venda venda, Produto produto, Integer quantidade, BigDecimal precoUnitario) {
        this.venda = venda;
        this.produto = produto;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
        recalcularSubtotal();
    }

    // ====== REGRAS DE NEGÓCIO ======

    public void recalcularSubtotal() {
        if (precoUnitario != null && quantidade != null) {
            this.subtotal = precoUnitario.multiply(BigDecimal.valueOf(quantidade));
        }
    }

    // ====== GETTERS / SETTERS ======

    public Long getId() {
        return id;
    }

    public Venda getVenda() {
        return venda;
    }

    public void setVenda(Venda venda) {
        this.venda = venda;
    }

    public Produto getProduto() {
        return produto;
    }

    public void setProduto(Produto produto) {
        this.produto = produto;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
        recalcularSubtotal();
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(BigDecimal precoUnitario) {
        this.precoUnitario = precoUnitario;
        recalcularSubtotal();
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, precoUnitario, produto, quantidade, subtotal, venda);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ItemVenda other = (ItemVenda) obj;
        return Objects.equals(id, other.id) && Objects.equals(precoUnitario, other.precoUnitario)
                && Objects.equals(produto, other.produto) && Objects.equals(quantidade, other.quantidade)
                && Objects.equals(subtotal, other.subtotal) && Objects.equals(venda, other.venda);
    }

    @Override
    public String toString() {
        return "ItemVenda [id=" + id + ", venda=" + venda + ", produto=" + produto + ", quantidade=" + quantidade
                + ", precoUnitario=" + precoUnitario + ", subtotal=" + subtotal + "]";
    }


}
