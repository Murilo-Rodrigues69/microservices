package isakids.isa.dto;

import java.math.BigDecimal;

public class ResumoVendaDTO {

    private BigDecimal totalBruto;
    private BigDecimal totalLiquido;
    private BigDecimal totalDesconto;
    private Long quantidadeVendas;

    public ResumoVendaDTO() {

    }

    public ResumoVendaDTO(BigDecimal totalBruto, BigDecimal totalLiquido, BigDecimal totalDesconto,
            Long quantidadeVendas) {
        super();
        this.totalBruto = totalBruto;
        this.totalLiquido = totalLiquido;
        this.totalDesconto = totalDesconto;
        this.quantidadeVendas = quantidadeVendas;
    }

    public BigDecimal getTotalBruto() {
        return totalBruto;
    }

    public void setTotalBruto(BigDecimal totalBruto) {
        this.totalBruto = totalBruto;
    }

    public BigDecimal getTotalLiquido() {
        return totalLiquido;
    }

    public void setTotalLiquido(BigDecimal totalLiquido) {
        this.totalLiquido = totalLiquido;
    }

    public BigDecimal getTotalDesconto() {
        return totalDesconto;
    }

    public void setTotalDesconto(BigDecimal totalDesconto) {
        this.totalDesconto = totalDesconto;
    }

    public Long getQuantidadeVendas() {
        return quantidadeVendas;
    }

    public void setQuantidadeVendas(Long quantidadeVendas) {
        this.quantidadeVendas = quantidadeVendas;
    }


}
