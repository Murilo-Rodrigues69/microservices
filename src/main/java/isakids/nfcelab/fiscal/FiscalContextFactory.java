package isakids.nfcelab.fiscal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fincatto.documentofiscal.nfe400.webservices.WSFacade;

import isakids.nfcelab.config.NfceIssuerProperties;
import isakids.nfcelab.entity.EmpresaEntity;

@Service
public class FiscalContextFactory {

    private final boolean wsEnabled;

    public FiscalContextFactory(@Value("${app.nfce.ws-enabled:true}") boolean wsEnabled) {
        this.wsEnabled = wsEnabled;
    }

    public FiscalContext create(EmpresaEntity empresa) throws Exception {
        EmpresaNfeConfig nfeConfig = new EmpresaNfeConfig(empresa);
        NfceIssuerProperties issuerProperties = mapIssuerProperties(empresa);
        WSFacade wsFacade = wsEnabled ? new WSFacade(nfeConfig) : null;
        return new FiscalContext(empresa, nfeConfig, issuerProperties, wsFacade);
    }

    private NfceIssuerProperties mapIssuerProperties(EmpresaEntity empresa) {
        NfceIssuerProperties issuerProperties = new NfceIssuerProperties();
        issuerProperties.setCnpj(empresa.getCnpj());
        issuerProperties.setRazaoSocial(empresa.getRazaoSocial());
        issuerProperties.setNomeFantasia(empresa.getNomeFantasia());
        issuerProperties.setInscricaoEstadual(empresa.getInscricaoEstadual());
        issuerProperties.setSerie(empresa.getSerie());
        issuerProperties.setNaturezaOperacao(empresa.getNaturezaOperacao());
        issuerProperties.setCep(empresa.getCep());
        issuerProperties.setLogradouro(empresa.getLogradouro());
        issuerProperties.setNumero(StringUtils.hasText(empresa.getNumero()) ? empresa.getNumero() : "S/N");
        issuerProperties.setBairro(empresa.getBairro());
        issuerProperties.setCodigoMunicipio(empresa.getCodigoMunicipio());
        issuerProperties.setDescricaoMunicipio(empresa.getDescricaoMunicipio());
        return issuerProperties;
    }
}
