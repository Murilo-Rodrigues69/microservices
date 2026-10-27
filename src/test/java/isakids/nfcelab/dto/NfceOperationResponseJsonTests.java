package isakids.nfcelab.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class NfceOperationResponseJsonTests {

    @Test
    void serializaCStatComNomeCamelCaseEsperadoPeloIsa() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

        NfceOperationResponse response = new NfceOperationResponse();
        response.setCStat(100);
        response.setMotivo("Autorizado o uso da NF-e");

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("\"cStat\":100");
        assertThat(json).doesNotContain("\"cstat\":100");
    }
}
