package isakids.nfcelab.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import isakids.nfcelab.service.DocumentoFiscalXmlParser.DocumentoFiscalResumo;
import isakids.nfcelab.support.SampleAuthorizedXmls;

class DocumentoFiscalXmlParserTests {

    private final DocumentoFiscalXmlParser parser = new DocumentoFiscalXmlParser();

    @Test
    void parseExtraiResumoDaNotaAutorizada() {
        DocumentoFiscalResumo resumo = parser.parse(SampleAuthorizedXmls.authorizedNfe()).orElseThrow();

        assertThat(resumo.chaveAcesso()).isEqualTo("52260411222333000144550010000020011000020010");
        assertThat(resumo.numeroNota()).isEqualTo("2001");
        assertThat(resumo.serie()).isEqualTo("1");
        assertThat(resumo.emitenteCnpj()).isEqualTo("11222333000144");
        assertThat(resumo.destinatarioNome()).isEqualTo("CLIENTE DEMONSTRACAO");
        assertThat(resumo.destinatarioDocumento()).isEqualTo("12345678901");
        assertThat(resumo.protocoloAutorizacao()).isEqualTo("152260000000001");
        assertThat(resumo.valorTotal()).isEqualByComparingTo(new BigDecimal("125.00"));
        assertThat(resumo.dataEmissao()).isEqualTo(LocalDateTime.of(2026, 4, 28, 13, 0));
        assertThat(resumo.observacao()).isNull();
    }

    @Test
    void parseExtraiObservacaoDeInfCpl() {
        String xml = """
                <NFe xmlns="http://www.portalfiscal.inf.br/nfe">
                  <infNFe Id="NFe52260411222333000144550010000020011000020010">
                    <ide>
                      <nNF>2001</nNF>
                      <serie>1</serie>
                      <dhEmi>2026-04-28T13:00:00-03:00</dhEmi>
                    </ide>
                    <emit>
                      <CNPJ>11222333000144</CNPJ>
                    </emit>
                    <infAdic>
                      <infCpl>Entregar no caixa lateral.</infCpl>
                    </infAdic>
                  </infNFe>
                </NFe>
                """;

        DocumentoFiscalResumo resumo = parser.parse(xml).orElseThrow();

        assertThat(resumo.chaveAcesso()).isEqualTo("52260411222333000144550010000020011000020010");
        assertThat(resumo.observacao()).isEqualTo("Entregar no caixa lateral.");
    }
}
