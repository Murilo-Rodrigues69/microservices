package isakids.nfcelab.support;

import java.nio.charset.StandardCharsets;

public final class SampleAuthorizedXmls {

    private SampleAuthorizedXmls() {
    }

    public static String authorizedNfe() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <nfeProc xmlns="http://www.portalfiscal.inf.br/nfe" versao="4.00">
                  <NFe>
                    <infNFe Id="NFe52260411222333000144550010000020011000020010" versao="4.00">
                      <ide>
                        <cUF>52</cUF>
                        <cNF>00002001</cNF>
                        <natOp>VENDA DE MERCADORIA</natOp>
                        <mod>55</mod>
                        <serie>1</serie>
                        <nNF>2001</nNF>
                        <dhEmi>2026-04-28T13:00:00-03:00</dhEmi>
                      </ide>
                      <emit>
                        <CNPJ>11222333000144</CNPJ>
                        <xNome>EMPRESA DEMONSTRACAO LTDA</xNome>
                        <xFant>LOJA DEMONSTRACAO</xFant>
                        <IE>123456789</IE>
                        <enderEmit>
                          <xLgr>AVENIDA EXEMPLO</xLgr>
                          <nro>0</nro>
                          <xBairro>BAIRRO EXEMPLO</xBairro>
                          <cMun>5214507</cMun>
                          <xMun>NEROPOLIS</xMun>
                          <UF>GO</UF>
                          <CEP>75460000</CEP>
                        </enderEmit>
                      </emit>
                      <dest>
                        <CPF>12345678901</CPF>
                        <xNome>CLIENTE DEMONSTRACAO</xNome>
                        <enderDest>
                          <xLgr>RUA TESTE</xLgr>
                          <nro>123</nro>
                          <xBairro>CENTRO</xBairro>
                          <cMun>5214507</cMun>
                          <xMun>NEROPOLIS</xMun>
                          <UF>GO</UF>
                          <CEP>75460000</CEP>
                        </enderDest>
                      </dest>
                      <det nItem="1">
                        <prod>
                          <cProd>9001</cProd>
                          <xProd>TIJOLO 9X19X19</xProd>
                          <NCM>69041000</NCM>
                          <CFOP>5102</CFOP>
                          <uCom>UN</uCom>
                          <qCom>100.0000</qCom>
                          <vUnCom>1.2500000000</vUnCom>
                          <vProd>125.00</vProd>
                        </prod>
                      </det>
                      <total>
                        <ICMSTot>
                          <vProd>125.00</vProd>
                          <vDesc>0.00</vDesc>
                          <vNF>125.00</vNF>
                        </ICMSTot>
                      </total>
                      <infNFeSupl>
                        <qrCode>https://www.sefaz.go.gov.br/nfe/consulta?chNFe=52260411222333000144550010000020011000020010</qrCode>
                      </infNFeSupl>
                    </infNFe>
                  </NFe>
                  <protNFe versao="4.00">
                    <infProt>
                      <tpAmb>2</tpAmb>
                      <verAplic>GO_TESTE</verAplic>
                      <chNFe>52260411222333000144550010000020011000020010</chNFe>
                      <dhRecbto>2026-04-28T13:00:05-03:00</dhRecbto>
                      <nProt>152260000000001</nProt>
                      <digVal>ABCDEF123456=</digVal>
                      <cStat>100</cStat>
                      <xMotivo>Autorizado o uso da NF-e</xMotivo>
                    </infProt>
                  </protNFe>
                </nfeProc>
                """;
    }

    public static String authorizedNfce() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <nfeProc xmlns="http://www.portalfiscal.inf.br/nfe" versao="4.00">
                  <NFe>
                    <infNFe Id="NFe52260411222333000144650010000020011000020010" versao="4.00">
                      <ide>
                        <cUF>52</cUF>
                        <cNF>00002001</cNF>
                        <natOp>VENDA DE MERCADORIA</natOp>
                        <mod>65</mod>
                        <serie>1</serie>
                        <nNF>2001</nNF>
                        <dhEmi>2026-04-28T13:00:00-03:00</dhEmi>
                        <tpAmb>2</tpAmb>
                      </ide>
                      <emit>
                        <CNPJ>11222333000144</CNPJ>
                        <xNome>EMPRESA DEMONSTRACAO LTDA</xNome>
                        <xFant>LOJA DEMONSTRACAO</xFant>
                        <IE>123456789</IE>
                        <enderEmit>
                          <xLgr>AVENIDA EXEMPLO</xLgr>
                          <nro>0</nro>
                          <xBairro>BAIRRO EXEMPLO</xBairro>
                          <cMun>5214507</cMun>
                          <xMun>NEROPOLIS</xMun>
                          <UF>GO</UF>
                          <CEP>75460000</CEP>
                        </enderEmit>
                      </emit>
                      <dest>
                        <CPF>12345678901</CPF>
                        <xNome>CLIENTE DEMONSTRACAO</xNome>
                      </dest>
                      <det nItem="1">
                        <prod>
                          <cProd>9001</cProd>
                          <xProd>TIJOLO 9X19X19</xProd>
                          <NCM>69041000</NCM>
                          <CFOP>5102</CFOP>
                          <uCom>UN</uCom>
                          <qCom>100.0000</qCom>
                          <vUnCom>1.2500000000</vUnCom>
                          <vProd>125.00</vProd>
                        </prod>
                      </det>
                      <total>
                        <ICMSTot>
                          <vProd>125.00</vProd>
                          <vDesc>0.00</vDesc>
                          <vNF>125.00</vNF>
                          <vTotTrib>12.34</vTotTrib>
                        </ICMSTot>
                      </total>
                      <pag>
                        <detPag>
                          <tPag>17</tPag>
                          <vPag>125.00</vPag>
                        </detPag>
                      </pag>
                      <infAdic>
                        <infCpl>OBSERVACAO TESTE NFC-E</infCpl>
                      </infAdic>
                      <infNFeSupl>
                        <qrCode>https://www.sefaz.go.gov.br/nfe/consulta?chNFe=52260411222333000144650010000020011000020010</qrCode>
                        <urlChave>https://www.sefaz.go.gov.br/nfe/consulta</urlChave>
                      </infNFeSupl>
                    </infNFe>
                  </NFe>
                  <protNFe versao="4.00">
                    <infProt>
                      <tpAmb>2</tpAmb>
                      <verAplic>GO_TESTE</verAplic>
                      <chNFe>52260411222333000144650010000020011000020010</chNFe>
                      <dhRecbto>2026-04-28T13:00:05-03:00</dhRecbto>
                      <nProt>152260000000001</nProt>
                      <digVal>ABCDEF123456=</digVal>
                      <cStat>100</cStat>
                      <xMotivo>Autorizado o uso da NFC-e</xMotivo>
                    </infProt>
                  </protNFe>
                </nfeProc>
                """;
    }

    public static byte[] minimalPdfBytes() {
        return "%PDF-1.4\n1 0 obj\n<<>>\nendobj\ntrailer\n<<>>\n%%EOF".getBytes(StandardCharsets.ISO_8859_1);
    }
}
