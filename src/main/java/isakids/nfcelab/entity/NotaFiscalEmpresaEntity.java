package isakids.nfcelab.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "nota_fiscal_empresa", uniqueConstraints = {
        @UniqueConstraint(name = "uk_nota_fiscal_empresa_chave", columnNames = "chave_acesso"),
        @UniqueConstraint(name = "uk_nota_fiscal_empresa_origem_venda",
                columnNames = { "empresa_id", "origem_sistema", "venda_origem_id" })
})
public class NotaFiscalEmpresaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private EmpresaEntity empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_empresa_id")
    private ClienteEmpresaEntity cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_emissor_id")
    private AppUserEntity usuarioEmissor;

    @Column(name = "usuario_emissor_login", length = 120)
    private String usuarioEmissorLogin;

    @Column(name = "usuario_emissor_nome", length = 255)
    private String usuarioEmissorNome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FiscalModelo modelo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SituacaoNotaFiscal situacao = SituacaoNotaFiscal.DESCONHECIDA;

    @Column(name = "numero_nota", length = 20)
    private String numeroNota;

    @Column(length = 10)
    private String serie;

    @Column(name = "chave_acesso", nullable = false, length = 44)
    private String chaveAcesso;

    @Column(name = "protocolo_autorizacao", length = 50)
    private String protocoloAutorizacao;

    @Column(name = "c_stat")
    private Integer cStat;

    @Column(length = 2000)
    private String motivo;

    @Column(length = 5000)
    private String observacao;

    @Column(name = "qr_code_url", length = 2000)
    private String qrCodeUrl;

    @Column(name = "destinatario_nome", length = 255)
    private String destinatarioNome;

    @Column(name = "destinatario_documento", length = 14)
    private String destinatarioDocumento;

    @Column(name = "valor_total", precision = 19, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "data_emissao")
    private LocalDateTime dataEmissao;

    @Column(name = "origem_sistema", length = 40)
    private String origemSistema;

    @Column(name = "venda_origem_id")
    private Long vendaOrigemId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public EmpresaEntity getEmpresa() {
        return empresa;
    }

    public void setEmpresa(EmpresaEntity empresa) {
        this.empresa = empresa;
    }

    public ClienteEmpresaEntity getCliente() {
        return cliente;
    }

    public void setCliente(ClienteEmpresaEntity cliente) {
        this.cliente = cliente;
    }

    public AppUserEntity getUsuarioEmissor() {
        return usuarioEmissor;
    }

    public void setUsuarioEmissor(AppUserEntity usuarioEmissor) {
        this.usuarioEmissor = usuarioEmissor;
    }

    public String getUsuarioEmissorLogin() {
        return usuarioEmissorLogin;
    }

    public void setUsuarioEmissorLogin(String usuarioEmissorLogin) {
        this.usuarioEmissorLogin = usuarioEmissorLogin;
    }

    public String getUsuarioEmissorNome() {
        return usuarioEmissorNome;
    }

    public void setUsuarioEmissorNome(String usuarioEmissorNome) {
        this.usuarioEmissorNome = usuarioEmissorNome;
    }

    public FiscalModelo getModelo() {
        return modelo;
    }

    public void setModelo(FiscalModelo modelo) {
        this.modelo = modelo;
    }

    public SituacaoNotaFiscal getSituacao() {
        return situacao;
    }

    public void setSituacao(SituacaoNotaFiscal situacao) {
        this.situacao = situacao;
    }

    public String getNumeroNota() {
        return numeroNota;
    }

    public void setNumeroNota(String numeroNota) {
        this.numeroNota = numeroNota;
    }

    public String getSerie() {
        return serie;
    }

    public void setSerie(String serie) {
        this.serie = serie;
    }

    public String getChaveAcesso() {
        return chaveAcesso;
    }

    public void setChaveAcesso(String chaveAcesso) {
        this.chaveAcesso = chaveAcesso;
    }

    public String getProtocoloAutorizacao() {
        return protocoloAutorizacao;
    }

    public void setProtocoloAutorizacao(String protocoloAutorizacao) {
        this.protocoloAutorizacao = protocoloAutorizacao;
    }

    public Integer getcStat() {
        return cStat;
    }

    public void setcStat(Integer cStat) {
        this.cStat = cStat;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public String getQrCodeUrl() {
        return qrCodeUrl;
    }

    public void setQrCodeUrl(String qrCodeUrl) {
        this.qrCodeUrl = qrCodeUrl;
    }

    public String getDestinatarioNome() {
        return destinatarioNome;
    }

    public void setDestinatarioNome(String destinatarioNome) {
        this.destinatarioNome = destinatarioNome;
    }

    public String getDestinatarioDocumento() {
        return destinatarioDocumento;
    }

    public void setDestinatarioDocumento(String destinatarioDocumento) {
        this.destinatarioDocumento = destinatarioDocumento;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public LocalDateTime getDataEmissao() {
        return dataEmissao;
    }

    public void setDataEmissao(LocalDateTime dataEmissao) {
        this.dataEmissao = dataEmissao;
    }

    public String getOrigemSistema() {
        return origemSistema;
    }

    public void setOrigemSistema(String origemSistema) {
        this.origemSistema = origemSistema;
    }

    public Long getVendaOrigemId() {
        return vendaOrigemId;
    }

    public void setVendaOrigemId(Long vendaOrigemId) {
        this.vendaOrigemId = vendaOrigemId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
