package isakids.isa.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class FiscalNfceResponseTests {

    @Test
    void desserializaPayloadComPdfFilePathSemFalhar() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

        String json = """
                {
                  "empresaId": 2,
                  "empresaNome": "Isakids e Teens",
                  "modelo": "NFCE",
                  "cStat": 100,
                  "motivo": "Autorizado o uso da NF-e",
                  "chaveAcesso": "52260511222333000144650010000000551000000055",
                  "protocoloAutorizacao": "123456789012345",
                  "qrCodeUrl": "https://sefaz/go/qr",
                  "pdfFilePath": "/tmp/nfce/522605.pdf",
                  "campoNovoIgnorado": "qualquer valor"
                }
                """;

        FiscalNfceResponse response = objectMapper.readValue(json, FiscalNfceResponse.class);

        assertThat(response.getEmpresaId()).isEqualTo(2L);
        assertThat(response.getCStat()).isEqualTo(100);
        assertThat(response.getChaveAcesso()).isEqualTo("52260511222333000144650010000000551000000055");
        assertThat(response.getPdfFilePath()).isEqualTo("/tmp/nfce/522605.pdf");
    }

    @Test
    void desserializaPayloadLegadoComCampoCstatMinusculo() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

        String json = """
                {
                  "empresaId": 2,
                  "cstat": 100,
                  "motivo": "Autorizado o uso da NF-e"
                }
                """;

        FiscalNfceResponse response = objectMapper.readValue(json, FiscalNfceResponse.class);

        assertThat(response.getCStat()).isEqualTo(100);
    }
}
