package isakids.nfcelab.dto;

import isakids.nfcelab.entity.FiscalAmbiente;
import isakids.nfcelab.entity.FiscalModelo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class EmpresaRequest {

    @NotBlank
    private String nome;

    @NotBlank
    private String cnpj;

    @NotBlank
    private String razaoSocial;

    private String nomeFantasia;

    @NotBlank
    private String inscricaoEstadual;

    @NotBlank
    private String serie;

    @NotBlank
    private String naturezaOperacao;

    @NotBlank
    private String cep;

    @NotBlank
    private String logradouro;

    @NotBlank
    private String numero;

    @NotBlank
    private String bairro;

    @NotBlank
    private String codigoMunicipio;

    @NotBlank
    private String descricaoMunicipio;

    @NotBlank
    private String uf;

    @NotNull
    private FiscalModelo modelo;

    @NotNull
    private FiscalAmbiente ambiente;

    @NotBlank
    private String certificadoPfxPath;

    private String certificadoSenha;

    @NotBlank
    private String cadeiaJksPath;

    private String cadeiaSenha;

    private String csc;

    private Integer cscId;

    private Boolean ativo;

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

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }
}
