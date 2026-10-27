package isakids.nfcelab.dto;

public class NfceStatusResponse {

    private Long empresaId;
    private String empresaNome;
    private String modelo;
    private String ambiente;
    private String uf;
    private Integer cStat;
    private String motivo;
    private String versaoAplicacao;

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

    public String getAmbiente() {
        return ambiente;
    }

    public void setAmbiente(String ambiente) {
        this.ambiente = ambiente;
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("cStat")
    public Integer getCStat() {
        return cStat;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("cStat")
    public void setCStat(Integer cStat) {
        this.cStat = cStat;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getVersaoAplicacao() {
        return versaoAplicacao;
    }

    public void setVersaoAplicacao(String versaoAplicacao) {
        this.versaoAplicacao = versaoAplicacao;
    }
}
