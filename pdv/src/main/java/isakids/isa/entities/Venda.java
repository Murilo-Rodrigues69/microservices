package isakids.isa.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import isakids.isa.enums.FormaPagamento;
import isakids.isa.enums.StatusVenda;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "vendas")
public class Venda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @Column(name = "total_bruto", precision = 15, scale = 2)
    private BigDecimal totalBruto = BigDecimal.ZERO;

    @Column(name = "desconto", precision = 15, scale = 2)
    private BigDecimal desconto = BigDecimal.ZERO;

    @Column(name = "total_liquido", precision = 15, scale = 2)
    private BigDecimal totalLiquido = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    private FormaPagamento formaPagamento;

    @Enumerated(EnumType.STRING)
    private StatusVenda status = StatusVenda.ABERTA;

    @Column(name = "cpf_cnpj_consumidor", length = 14)
    private String cpfCnpjConsumidor;

    @Column(name = "nota_fiscal_empresa_id")
    private Long notaFiscalEmpresaId;

    @Column(name = "nota_fiscal_empresa_nome", length = 255)
    private String notaFiscalEmpresaNome;

    @Column(name = "nota_fiscal_modelo", length = 10)
    private String notaFiscalModelo;

    @Column(name = "nota_fiscal_situacao", length = 30)
    private String notaFiscalSituacao;

    @Column(name = "nota_fiscal_c_stat")
    private Integer notaFiscalCStat;

    @Column(name = "nota_fiscal_motivo", length = 2000)
    private String notaFiscalMotivo;

    @Column(name = "nota_fiscal_chave_acesso", length = 44)
    private String notaFiscalChaveAcesso;

    @Column(name = "nota_fiscal_protocolo_autorizacao", length = 50)
    private String notaFiscalProtocoloAutorizacao;

    @Column(name = "nota_fiscal_qr_code_url", length = 2000)
    private String notaFiscalQrCodeUrl;

    @Column(name = "nota_fiscal_data_emissao")
    private LocalDateTime notaFiscalDataEmissao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loja_id", nullable = false)
    private Loja loja;

    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemVenda> itens = new ArrayList<>();

    public Venda() {
        this.dataHora = LocalDateTime.now();
    }

    public Venda(Long id, LocalDateTime dataHora, BigDecimal totalBruto, BigDecimal desconto, BigDecimal totalLiquido,
            FormaPagamento formaPagamento, StatusVenda status, Usuario usuario, Loja loja, List<ItemVenda> itens) {
        this.id = id;
        this.dataHora = dataHora;
        this.totalBruto = totalBruto;
        this.desconto = desconto;
        this.totalLiquido = totalLiquido;
        this.formaPagamento = formaPagamento;
        this.status = status;
        this.usuario = usuario;
        this.loja = loja;
        this.itens = itens;
    }

    public void adicionarItem(ItemVenda item) {
        item.setVenda(this);
        itens.add(item);
        recalcularTotais();
    }

    public void recalcularTotais() {
        totalBruto = itens.stream()
                .map(ItemVenda::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        totalLiquido = totalBruto.subtract(desconto != null ? desconto : BigDecimal.ZERO);
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public BigDecimal getTotalBruto() {
        return totalBruto;
    }

    public BigDecimal getDesconto() {
        return desconto;
    }

    public void setDesconto(BigDecimal desconto) {
        this.desconto = desconto;
        recalcularTotais();
    }

    public BigDecimal getTotalLiquido() {
        return totalLiquido;
    }

    public FormaPagamento getFormaPagamento() {
        return formaPagamento;
    }

    public void setFormaPagamento(FormaPagamento formaPagamento) {
        this.formaPagamento = formaPagamento;
    }

    public StatusVenda getStatus() {
        return status;
    }

    public void setStatus(StatusVenda status) {
        this.status = status;
    }

    public String getCpfCnpjConsumidor() {
        return cpfCnpjConsumidor;
    }

    public void setCpfCnpjConsumidor(String cpfCnpjConsumidor) {
        this.cpfCnpjConsumidor = cpfCnpjConsumidor;
    }

    public Long getNotaFiscalEmpresaId() {
        return notaFiscalEmpresaId;
    }

    public void setNotaFiscalEmpresaId(Long notaFiscalEmpresaId) {
        this.notaFiscalEmpresaId = notaFiscalEmpresaId;
    }

    public String getNotaFiscalEmpresaNome() {
        return notaFiscalEmpresaNome;
    }

    public void setNotaFiscalEmpresaNome(String notaFiscalEmpresaNome) {
        this.notaFiscalEmpresaNome = notaFiscalEmpresaNome;
    }

    public String getNotaFiscalModelo() {
        return notaFiscalModelo;
    }

    public void setNotaFiscalModelo(String notaFiscalModelo) {
        this.notaFiscalModelo = notaFiscalModelo;
    }

    public String getNotaFiscalSituacao() {
        String situacaoPersistida = trimToNull(notaFiscalSituacao);
        if (situacaoPersistida != null && !"DESCONHECIDA".equalsIgnoreCase(situacaoPersistida)) {
            return situacaoPersistida;
        }
        String situacaoPorCStat = resolveSituacaoFiscal(notaFiscalCStat, null);
        if (situacaoPorCStat != null) {
            return situacaoPorCStat;
        }
        return resolveSituacaoFiscalLegada(situacaoPersistida);
    }

    public void setNotaFiscalSituacao(String notaFiscalSituacao) {
        this.notaFiscalSituacao = notaFiscalSituacao;
    }

    public Integer getNotaFiscalCStat() {
        return notaFiscalCStat;
    }

    public void setNotaFiscalCStat(Integer notaFiscalCStat) {
        this.notaFiscalCStat = notaFiscalCStat;
    }

    public String getNotaFiscalMotivo() {
        return notaFiscalMotivo;
    }

    public void setNotaFiscalMotivo(String notaFiscalMotivo) {
        this.notaFiscalMotivo = notaFiscalMotivo;
    }

    public String getNotaFiscalChaveAcesso() {
        return notaFiscalChaveAcesso;
    }

    public void setNotaFiscalChaveAcesso(String notaFiscalChaveAcesso) {
        this.notaFiscalChaveAcesso = notaFiscalChaveAcesso;
    }

    public String getNotaFiscalProtocoloAutorizacao() {
        return notaFiscalProtocoloAutorizacao;
    }

    public void setNotaFiscalProtocoloAutorizacao(String notaFiscalProtocoloAutorizacao) {
        this.notaFiscalProtocoloAutorizacao = notaFiscalProtocoloAutorizacao;
    }

    public String getNotaFiscalQrCodeUrl() {
        return notaFiscalQrCodeUrl;
    }

    public void setNotaFiscalQrCodeUrl(String notaFiscalQrCodeUrl) {
        this.notaFiscalQrCodeUrl = notaFiscalQrCodeUrl;
    }

    public LocalDateTime getNotaFiscalDataEmissao() {
        return notaFiscalDataEmissao;
    }

    public void setNotaFiscalDataEmissao(LocalDateTime notaFiscalDataEmissao) {
        this.notaFiscalDataEmissao = notaFiscalDataEmissao;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Loja getLoja() {
        return loja;
    }

    public void setLoja(Loja loja) {
        this.loja = loja;
    }

    public List<ItemVenda> getItens() {
        return itens;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public void setTotalBruto(BigDecimal totalBruto) {
        this.totalBruto = totalBruto;
    }

    public void setTotalLiquido(BigDecimal totalLiquido) {
        this.totalLiquido = totalLiquido;
    }

    public void setItens(List<ItemVenda> itens) {
        this.itens = itens;
    }

    @Transient
    public String getNfcePdfDownloadUrl() {
        return possuiNfceEmitida() ? "/api/pdv/vendas/" + id + "/nfce/pdf" : null;
    }

    @Transient
    public String getNfcePdfTermicoDownloadUrl() {
        return possuiNfceEmitida() ? "/api/pdv/vendas/" + id + "/nfce/pdf-termico" : null;
    }

    @Transient
    public String getNfceXmlDownloadUrl() {
        return possuiNfceEmitida() ? "/api/pdv/vendas/" + id + "/nfce/xml" : null;
    }

    private boolean possuiNfceEmitida() {
        return notaFiscalEmpresaId != null
                || (notaFiscalChaveAcesso != null && !notaFiscalChaveAcesso.isBlank());
    }

    private String resolveSituacaoFiscal(Integer cStat, String fallback) {
        if (cStat == null) {
            return fallback;
        }
        return switch (cStat) {
            case 100, 150 -> "AUTORIZADA";
            case 101, 135, 151, 155 -> "CANCELADA";
            default -> {
                if (cStat >= 500) {
                    yield "ERRO";
                }
                if (cStat >= 200) {
                    yield "NAO_AUTORIZADA";
                }
                yield "PENDENTE";
            }
        };
    }

    private String resolveSituacaoFiscalLegada(String fallback) {
        String motivo = trimToNull(notaFiscalMotivo);
        if (trimToNull(notaFiscalProtocoloAutorizacao) != null
                && motivo != null
                && motivo.toUpperCase().startsWith("AUTORIZADO O USO DA NF")) {
            return "AUTORIZADA";
        }
        return fallback;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
