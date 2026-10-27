package isakids.nfcelab.fiscal;

import isakids.nfcelab.entity.EmpresaEntity;
import isakids.nfcelab.model.RecipientData;
import isakids.nfcelab.model.SaleData;

public interface NfceEmitter {

    NfceEmissionResult emitir(EmpresaEntity empresa, SaleData venda, RecipientData destinatario);

    NfceEmissionResult consultarPorChave(EmpresaEntity empresa, String chaveAcesso);

    NfceEmissionResult cancelar(EmpresaEntity empresa, String chaveAcesso, String protocolo, String justificativa);
}
