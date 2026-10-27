package isakids.nfcelab.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import isakids.nfcelab.entity.FiscalAmbiente;
import isakids.nfcelab.entity.FiscalModelo;

@ConfigurationProperties(prefix = "nfe")
public class LegacyFiscalBootstrapProperties {

    private FiscalModelo modelo = FiscalModelo.NFE;
    private FiscalAmbiente ambiente = FiscalAmbiente.HOMOLOGACAO;
    private String uf = "GO";
    private final Certificado certificado = new Certificado();
    private final Cadeia cadeia = new Cadeia();
    private String csc;
    private Integer cscId = 0;

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

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }

    public Certificado getCertificado() {
        return certificado;
    }

    public Cadeia getCadeia() {
        return cadeia;
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

    public static class Certificado {
        private String pfxPath;
        private String senha;

        public String getPfxPath() {
            return pfxPath;
        }

        public void setPfxPath(String pfxPath) {
            this.pfxPath = pfxPath;
        }

        public String getSenha() {
            return senha;
        }

        public void setSenha(String senha) {
            this.senha = senha;
        }
    }

    public static class Cadeia {
        private String jksPath;
        private String senha;

        public String getJksPath() {
            return jksPath;
        }

        public void setJksPath(String jksPath) {
            this.jksPath = jksPath;
        }

        public String getSenha() {
            return senha;
        }

        public void setSenha(String senha) {
            this.senha = senha;
        }
    }
}
