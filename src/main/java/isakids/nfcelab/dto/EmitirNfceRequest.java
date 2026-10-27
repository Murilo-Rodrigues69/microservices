package isakids.nfcelab.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import isakids.nfcelab.model.FormaPagamento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public class EmitirNfceRequest {

    private Long empresaId;
    private Long vendaId;
    private Long clienteEmpresaId;
    private String origemSistema;

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
    private DestinatarioRequest destinatario;

    @Valid
    @NotEmpty
    private List<NfceItemRequest> itens;

    public Long getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(Long empresaId) {
        this.empresaId = empresaId;
    }

    public Long getVendaId() {
        return vendaId;
    }

    public void setVendaId(Long vendaId) {
        this.vendaId = vendaId;
    }

    public Long getClienteEmpresaId() {
        return clienteEmpresaId;
    }

    public void setClienteEmpresaId(Long clienteEmpresaId) {
        this.clienteEmpresaId = clienteEmpresaId;
    }

    public String getOrigemSistema() {
        return origemSistema;
    }

    public void setOrigemSistema(String origemSistema) {
        this.origemSistema = origemSistema;
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

    public DestinatarioRequest getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(DestinatarioRequest destinatario) {
        this.destinatario = destinatario;
    }

    public List<NfceItemRequest> getItens() {
        return itens;
    }

    public void setItens(List<NfceItemRequest> itens) {
        this.itens = itens;
    }
}
