package isakids.isa.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FiscalNfceResponse {

    private Long empresaId;
    private String empresaNome;
    private String modelo;
    private Integer cStat;
    private String motivo;
    private String chaveAcesso;
    private String protocoloAutorizacao;
    private String qrCodeUrl;
    private String xmlDownloadUrl;
    private String pdfDownloadUrl;
    private String xmlFilePath;
    private String pdfFilePath;

    public Long getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(Long empresaId) {
        this.empresaId = empresaId;
    }

    public String getEmpresaNome() {
        return empresaNome;
    }

    public void setEmpresaNome(String empresaNome) {
        this.empresaNome = empresaNome;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    @JsonProperty("cStat")
    public Integer getCStat() {
        return cStat;
    }

    @JsonProperty("cStat")
    @JsonAlias("cstat")
    public void setCStat(Integer cStat) {
        this.cStat = cStat;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
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

    public String getQrCodeUrl() {
        return qrCodeUrl;
    }

    public void setQrCodeUrl(String qrCodeUrl) {
        this.qrCodeUrl = qrCodeUrl;
    }

    public String getXmlDownloadUrl() {
        return xmlDownloadUrl;
    }

    public void setXmlDownloadUrl(String xmlDownloadUrl) {
        this.xmlDownloadUrl = xmlDownloadUrl;
    }

    public String getPdfDownloadUrl() {
        return pdfDownloadUrl;
    }

    public void setPdfDownloadUrl(String pdfDownloadUrl) {
        this.pdfDownloadUrl = pdfDownloadUrl;
    }

    public String getXmlFilePath() {
        return xmlFilePath;
    }

    public void setXmlFilePath(String xmlFilePath) {
        this.xmlFilePath = xmlFilePath;
    }

    public String getPdfFilePath() {
        return pdfFilePath;
    }

    public void setPdfFilePath(String pdfFilePath) {
        this.pdfFilePath = pdfFilePath;
    }
}
