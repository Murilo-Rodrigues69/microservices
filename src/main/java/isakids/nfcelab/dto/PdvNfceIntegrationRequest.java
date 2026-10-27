package isakids.nfcelab.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import isakids.nfcelab.model.FormaPagamento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public class PdvNfceIntegrationRequest {

    @NotBlank
    private String lojaCnpj;

    @NotNull
    @Positive
    private Long vendaId;

    @Pattern(regexp = "\\d{1,9}")
    private String numeroNota;

    private LocalDateTime dataHora;

    @PositiveOrZero
    private BigDecimal desconto;

    @NotNull
    private FormaPagamento formaPagamento;

    @Size(max = 5000)
    private String observacao;

    private String cpfCnpjConsumidor;

    @Valid
    @NotEmpty
    private List<PdvNfceIntegrationItemRequest> itens;

    public String getLojaCnpj() {
        return lojaCnpj;
    }

    public void setLojaCnpj(String lojaCnpj) {
        this.lojaCnpj = lojaCnpj;
    }

    public Long getVendaId() {
        return vendaId;
    }

    public void setVendaId(Long vendaId) {
        this.vendaId = vendaId;
    }

    public String getNumeroNota() {
        return numeroNota;
    }

    public void setNumeroNota(String numeroNota) {
        this.numeroNota = numeroNota;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public BigDecimal getDesconto() {
        return desconto;
    }

    public void setDesconto(BigDecimal desconto) {
        this.desconto = desconto;
    }

    public FormaPagamento getFormaPagamento() {
        return formaPagamento;
    }

    public void setFormaPagamento(FormaPagamento formaPagamento) {
        this.formaPagamento = formaPagamento;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public String getCpfCnpjConsumidor() {
        return cpfCnpjConsumidor;
    }

    public void setCpfCnpjConsumidor(String cpfCnpjConsumidor) {
        this.cpfCnpjConsumidor = cpfCnpjConsumidor;
    }

    public List<PdvNfceIntegrationItemRequest> getItens() {
        return itens;
    }

    public void setItens(List<PdvNfceIntegrationItemRequest> itens) {
        this.itens = itens;
    }
}
