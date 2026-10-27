package isakids.nfcelab.fiscal;

public class NfceEmissionResult {

    private Integer cStat;
    private String motivo;
    private String chaveAcesso;
    private String protocoloAutorizacao;
    private String qrCodeUrl;
    private String xmlAutorizado;

    public NfceEmissionResult() {
    }

    public Integer getcStat() {
        return cStat;
    }

    public String getMotivo() {
        return motivo;
    }

    public String getChaveAcesso() {
        return chaveAcesso;
    }

    public String getProtocoloAutorizacao() {
        return protocoloAutorizacao;
    }

    public String getQrCodeUrl() {
        return qrCodeUrl;
    }

    public String getXmlAutorizado() {
        return xmlAutorizado;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final NfceEmissionResult result = new NfceEmissionResult();

        public Builder cStat(Integer cStat) {
            result.cStat = cStat;
            return this;
        }

        public Builder motivo(String motivo) {
            result.motivo = motivo;
            return this;
        }

        public Builder chaveAcesso(String chaveAcesso) {
            result.chaveAcesso = chaveAcesso;
            return this;
        }

        public Builder protocoloAutorizacao(String protocoloAutorizacao) {
            result.protocoloAutorizacao = protocoloAutorizacao;
            return this;
        }

        public Builder qrCodeUrl(String qrCodeUrl) {
            result.qrCodeUrl = qrCodeUrl;
            return this;
        }

        public Builder xmlAutorizado(String xmlAutorizado) {
            result.xmlAutorizado = xmlAutorizado;
            return this;
        }

        public NfceEmissionResult build() {
            return result;
        }
    }
}
