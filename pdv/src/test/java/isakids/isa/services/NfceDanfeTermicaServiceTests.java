package isakids.isa.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;

import isakids.isa.entities.ItemVenda;
import isakids.isa.entities.Loja;
import isakids.isa.entities.Produto;
import isakids.isa.entities.Usuario;
import isakids.isa.entities.Venda;
import isakids.isa.enums.FormaPagamento;
import isakids.isa.enums.StatusVenda;

class NfceDanfeTermicaServiceTests {

    @Test
    void geraPdfTermicoComLarguraDeBobinaETextoDaDanfe() throws Exception {
        NfceDanfeTermicaService service = new NfceDanfeTermicaService();
        Venda venda = vendaComItens(List.of(item("Vestido Infantil", new BigDecimal("49.90"), 2)));
        venda.setNotaFiscalEmpresaId(10L);
        venda.setNotaFiscalChaveAcesso("52260511222333000144650010000000551000000055");
        venda.setNotaFiscalSituacao("AUTORIZADA");
        venda.setNotaFiscalProtocoloAutorizacao("123456789012345");
        venda.setNotaFiscalQrCodeUrl("https://sefaz.go.gov.br/nfce?q=123");

        byte[] pdf = service.gerarDanfe(venda, xmlNfce());

        assertThat(new String(pdf, 0, 4, StandardCharsets.UTF_8)).isEqualTo("%PDF");

        PdfReader reader = new PdfReader(pdf);
        try {
            assertThat(reader.getNumberOfPages()).isEqualTo(1);
            assertThat(reader.getPageSize(1).getWidth()).isLessThan(230f);

            String pageText = new PdfTextExtractor(reader).getTextFromPage(1);
            assertThat(pageText).contains("DANFE NFC-e");
            assertThat(pageText).contains("Loja Centro");
            assertThat(pageText).contains("Vestido Infantil");
            assertThat(pageText).contains("CHAVE DE ACESSO");
            assertThat(pageText).contains("PIX");
        } finally {
            reader.close();
        }
    }

    private byte[] xmlNfce() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <nfeProc xmlns="http://www.portalfiscal.inf.br/nfe" versao="4.00">
                  <NFe>
                    <infNFe Id="NFe52260511222333000144650010000000551000000055">
                      <ide>
                        <serie>1</serie>
                        <nNF>55</nNF>
                        <dhEmi>2026-05-26T14:30:00-03:00</dhEmi>
                      </ide>
                      <emit>
                        <CNPJ>11222333000144</CNPJ>
                        <xNome>Loja Centro</xNome>
                        <IE>123456789</IE>
                        <enderEmit>
                          <xLgr>Rua 1</xLgr>
                          <nro>100</nro>
                          <xBairro>Centro</xBairro>
                          <xMun>Goiania</xMun>
                          <UF>GO</UF>
                          <CEP>74000000</CEP>
                        </enderEmit>
                      </emit>
                      <dest>
                        <CPF>12345678901</CPF>
                        <xNome>Cliente Teste</xNome>
                      </dest>
                      <det nItem="1">
                        <prod>
                          <cProd>20</cProd>
                          <xProd>Vestido Infantil</xProd>
                          <qCom>2.0000</qCom>
                          <uCom>UN</uCom>
                          <vUnCom>49.90</vUnCom>
                          <vProd>99.80</vProd>
                        </prod>
                        <imposto>
                          <vTotTrib>12.34</vTotTrib>
                        </imposto>
                      </det>
                      <total>
                        <ICMSTot>
                          <vProd>99.80</vProd>
                          <vDesc>0.00</vDesc>
                          <vNF>99.80</vNF>
                          <vTotTrib>12.34</vTotTrib>
                        </ICMSTot>
                      </total>
                      <pag>
                        <detPag>
                          <tPag>17</tPag>
                          <vPag>99.80</vPag>
                        </detPag>
                      </pag>
                      <infAdic>
                        <infCpl>Documento emitido em ambiente de homologacao.</infCpl>
                      </infAdic>
                    </infNFe>
                    <infNFeSupl>
                      <qrCode>https://sefaz.go.gov.br/nfce?q=123</qrCode>
                    </infNFeSupl>
                  </NFe>
                  <protNFe>
                    <infProt>
                      <cStat>100</cStat>
                      <xMotivo>Autorizado o uso da NF-e</xMotivo>
                      <nProt>123456789012345</nProt>
                    </infProt>
                  </protNFe>
                </nfeProc>
                """.getBytes(StandardCharsets.UTF_8);
    }

    private Venda vendaComItens(List<ItemVenda> itens) {
        Loja loja = new Loja();
        loja.setId(10L);
        loja.setCodigo("CENTRO");
        loja.setCnpj("11222333000144");
        loja.setNome("Loja Centro");
        loja.setInscricaoEstadual("123456789");

        Usuario usuario = new Usuario();
        usuario.setNome("Operadora Teste");

        Venda venda = new Venda();
        venda.setId(1L);
        venda.setLoja(loja);
        venda.setUsuario(usuario);
        venda.setStatus(StatusVenda.CONCLUIDA);
        venda.setFormaPagamento(FormaPagamento.PIX);
        venda.setItens(itens);
        venda.setDesconto(BigDecimal.ZERO);
        for (ItemVenda item : itens) {
            item.setVenda(venda);
        }
        venda.recalcularTotais();
        return venda;
    }

    private ItemVenda item(String descricao, BigDecimal precoUnitario, int quantidade) {
        Produto produto = new Produto();
        produto.setId((long) descricao.hashCode());
        produto.setCodigoInterno("SKU-" + Math.abs(descricao.hashCode()));
        produto.setDescricao(descricao);
        produto.setCodigoBarras("7891234567890");
        produto.setNcm("62044300");
        produto.setOrigem("NACIONAL");
        produto.setCfop("5102");
        produto.setUnidadeComercial("UN");

        return new ItemVenda(null, produto, quantidade, precoUnitario);
    }
}
