package isakids.nfcelab.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "empresa", uniqueConstraints = {
        @UniqueConstraint(name = "uk_empresa_cnpj", columnNames = "cnpj")
})
public class EmpresaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 14)
    private String cnpj;

    @Column(name = "razao_social", nullable = false, length = 255)
    private String razaoSocial;

    @Column(name = "nome_fantasia", length = 255)
    private String nomeFantasia;

    @Column(name = "inscricao_estadual", nullable = false, length = 30)
    private String inscricaoEstadual;

    @Column(nullable = false, length = 10)
    private String serie = "1";

    @Column(name = "natureza_operacao", nullable = false, length = 255)
    private String naturezaOperacao = "VENDA DE MERCADORIA";

    @Column(nullable = false, length = 8)
    private String cep;

    @Column(nullable = false, length = 255)
    private String logradouro;

    @Column(nullable = false, length = 30)
    private String numero;

    @Column(nullable = false, length = 120)
    private String bairro;

    @Column(name = "codigo_municipio", nullable = false, length = 10)
    private String codigoMunicipio;

    @Column(name = "descricao_municipio", nullable = false, length = 120)
    private String descricaoMunicipio;

    @Column(nullable = false, length = 2)
    private String uf;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FiscalModelo modelo = FiscalModelo.NFE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FiscalAmbiente ambiente = FiscalAmbiente.HOMOLOGACAO;

    @Column(name = "certificado_pfx_path", nullable = false, length = 500)
    private String certificadoPfxPath;

    @Column(name = "certificado_senha", nullable = false, length = 255)
    private String certificadoSenha;

    @Column(name = "cadeia_jks_path", nullable = false, length = 500)
    private String cadeiaJksPath;

    @Column(name = "cadeia_senha", nullable = false, length = 255)
    private String cadeiaSenha;

    @Column(length = 255)
    private String csc;

    @Column(name = "csc_id")
    private Integer cscId;

    @Column(nullable = false)
    private boolean ativo = true;

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

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getRazaoSocial() {
        return razaoSocial;
    }

    public void setRazaoSocial(String razaoSocial) {
        this.razaoSocial = razaoSocial;
    }

    public String getNomeFantasia() {
        return nomeFantasia;
    }

    public void setNomeFantasia(String nomeFantasia) {
        this.nomeFantasia = nomeFantasia;
    }

    public String getInscricaoEstadual() {
        return inscricaoEstadual;
    }

    public void setInscricaoEstadual(String inscricaoEstadual) {
        this.inscricaoEstadual = inscricaoEstadual;
    }

    public String getSerie() {
        return serie;
    }

    public void setSerie(String serie) {
        this.serie = serie;
    }

    public String getNaturezaOperacao() {
        return naturezaOperacao;
    }

    public void setNaturezaOperacao(String naturezaOperacao) {
        this.naturezaOperacao = naturezaOperacao;
    }

    public String getCep() {
        return cep;
    }

    public void setCep(String cep) {
        this.cep = cep;
    }

    public String getLogradouro() {
        return logradouro;
    }

    public void setLogradouro(String logradouro) {
        this.logradouro = logradouro;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getBairro() {
        return bairro;
    }

    public void setBairro(String bairro) {
        this.bairro = bairro;
    }

    public String getCodigoMunicipio() {
        return codigoMunicipio;
    }

    public void setCodigoMunicipio(String codigoMunicipio) {
        this.codigoMunicipio = codigoMunicipio;
    }

    public String getDescricaoMunicipio() {
        return descricaoMunicipio;
    }

    public void setDescricaoMunicipio(String descricaoMunicipio) {
        this.descricaoMunicipio = descricaoMunicipio;
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }

    public FiscalModelo getModelo() {
        return modelo;
    }

    public void setModelo(FiscalModelo modelo) {
        this.modelo = modelo;
    }

    public FiscalAmbiente getAmbiente() {
        return ambiente;
    }

    public void setAmbiente(FiscalAmbiente ambiente) {
        this.ambiente = ambiente;
    }

    public String getCertificadoPfxPath() {
        return certificadoPfxPath;
    }

    public void setCertificadoPfxPath(String certificadoPfxPath) {
        this.certificadoPfxPath = certificadoPfxPath;
    }

    public String getCertificadoSenha() {
        return certificadoSenha;
    }

    public void setCertificadoSenha(String certificadoSenha) {
        this.certificadoSenha = certificadoSenha;
    }

    public String getCadeiaJksPath() {
        return cadeiaJksPath;
    }

    public void setCadeiaJksPath(String cadeiaJksPath) {
        this.cadeiaJksPath = cadeiaJksPath;
    }

    public String getCadeiaSenha() {
        return cadeiaSenha;
    }

    public void setCadeiaSenha(String cadeiaSenha) {
        this.cadeiaSenha = cadeiaSenha;
    }

    public String getCsc() {
        return csc;
    }

    public void setCsc(String csc) {
        this.csc = csc;
    }

    public Integer getCscId() {
        return cscId;
    }

    public void setCscId(Integer cscId) {
        this.cscId = cscId;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
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
