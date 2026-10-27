package isakids.nfcelab.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import isakids.nfcelab.fiscal.NfceEmissionResult;

public class NfceOperationResponse {

    private Long empresaId;
    private String empresaNome;
    private String modelo;
    private Integer cStat;
    private String motivo;
    private String observacao;
    private String chaveAcesso;
    private String protocoloAutorizacao;
    private String qrCodeUrl;
    private String xml;
    private String xmlDownloadUrl;
    private String pdfDownloadUrl;
    private String xmlFilePath;
    private String pdfFilePath;

    public static NfceOperationResponse fromResult(NfceEmissionResult result) {
        NfceOperationResponse response = new NfceOperationResponse();
        response.setCStat(result.getcStat());
        response.setMotivo(result.getMotivo());
        response.setChaveAcesso(result.getChaveAcesso());
        response.setProtocoloAutorizacao(result.getProtocoloAutorizacao());
        response.setQrCodeUrl(result.getQrCodeUrl());
        response.setXml(result.getXmlAutorizado());
        return response;
    }

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
    public void setCStat(Integer cStat) {
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

    public String getXml() {
        return xml;
    }

    public void setXml(String xml) {
        this.xml = xml;
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
