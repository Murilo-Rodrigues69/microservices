package isakids.isa.entities;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class VendaJsonTests {

    @Test
    void serializaSituacaoAutorizadaQuandoRegistroLegadoTemProtocoloEMotivoDeAutorizacao() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

        Venda venda = new Venda();
        venda.setNotaFiscalSituacao("DESCONHECIDA");
        venda.setNotaFiscalCStat(null);
        venda.setNotaFiscalProtocoloAutorizacao("123456789012345");
        venda.setNotaFiscalMotivo("Autorizado o uso da NF-e");

        String json = objectMapper.writeValueAsString(venda);

        assertThat(json).contains("\"notaFiscalSituacao\":\"AUTORIZADA\"");
    }
}
