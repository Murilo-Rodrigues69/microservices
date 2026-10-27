package isakids.nfcelab.fiscal;

import com.fincatto.documentofiscal.nfe.NFeConfig;
import com.fincatto.documentofiscal.nfe400.webservices.WSFacade;

import isakids.nfcelab.config.NfceIssuerProperties;
import isakids.nfcelab.entity.EmpresaEntity;

public record FiscalContext(
        EmpresaEntity empresa,
        NFeConfig nfeConfig,
        NfceIssuerProperties issuerProperties,
        WSFacade wsFacade) {
}
