package isakids.nfcelab.fiscal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fincatto.documentofiscal.DFAmbiente;
import com.fincatto.documentofiscal.DFModelo;
import com.fincatto.documentofiscal.DFUnidadeFederativa;
import com.fincatto.documentofiscal.nfe.NFeConfig;
import com.fincatto.documentofiscal.nfe400.classes.NFOrigem;
import com.fincatto.documentofiscal.nfe400.classes.NFTipoImpressao;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFIdentificadorLocalDestinoOperacao;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFIndicadorIEDestinatario;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFTipoIntegracaoPagamento;

import isakids.nfcelab.config.NfceIssuerProperties;
import isakids.nfcelab.model.FormaPagamento;
import isakids.nfcelab.model.ProductData;
import isakids.nfcelab.model.RecipientData;
import isakids.nfcelab.model.SaleData;
import isakids.nfcelab.model.SaleItemData;

class NfceXmlFactoryTests {

    @Test
    void geraNotaComModeloNfeEDadosFiscaisDoTijolo() {
        NfceXmlFactory factory = new NfceXmlFactory(new StubNfeConfig(DFModelo.NFE), issuerProperties());

        SaleData sale = new SaleData(
                1001L,
                "1001",
                LocalDateTime.of(2026, 4, 17, 14, 30),
                BigDecimal.valueOf(5),
                FormaPagamento.PIX,
                null,
                List.of(new SaleItemData(
                        1L,
                        2,
                        BigDecimal.valueOf(19.90),
                        null,
                        new ProductData(10L, "TIJOLO 9X19X19", null, "69041000", "10.027.00", "NACIONAL", "5102",
                                "UN"))));

        var nota = factory.gerarNotaFiscal(
                sale,
                new RecipientData(
                        "Murilo Oliveira Rodrigues",
                        "12345678901",
                        null,
                        "75460000",
                        "QSA 10 LOTE 01",
                        "10",
                        "TAGUATINGA SUL",
                        "5214507",
                        "NEROPOLIS",
                        "GO",
                        null));

        assertThat(nota.getInfo().getIdentificacao().getModelo()).isEqualTo(DFModelo.NFE);
        assertThat(nota.getInfo().getIdentificacao().getTipoImpressao()).isEqualTo(NFTipoImpressao.DANFE_NORMAL_RETRATO);
        assertThat(nota.getInfo().getIdentificacao().getUf()).isEqualTo(DFUnidadeFederativa.GO);
        assertThat(nota.getInfo().getEmitente().getCnpj()).isEqualTo("11222333000144");
        assertThat(nota.getInfo().getEmitente().getInscricaoEstadual()).isEqualTo("123456789");
        assertThat(nota.getInfo().getEmitente().getEndereco().getCodigoMunicipio()).isEqualTo("5214507");
        assertThat(nota.getInfo().getEmitente().getEndereco().getDescricaoMunicipio()).isEqualTo("NEROPOLIS");
        assertThat(nota.getInfo().getEmitente().getEndereco().getUf()).isEqualTo("GO");
        assertThat(nota.getInfo().getDestinatario().getCpf()).isEqualTo("12345678901");
        assertThat(nota.getInfo().getDestinatario().getIndicadorIEDestinatario())
                .isEqualTo(NFIndicadorIEDestinatario.NAO_CONTRIBUINTE);
        assertThat(nota.getInfo().getDestinatario().getRazaoSocial())
                .isEqualTo("NF-E EMITIDA EM AMBIENTE DE HOMOLOGACAO - SEM VALOR FISCAL");
        assertThat(nota.getInfo().getItens()).hasSize(1);
        assertThat(nota.getInfo().getItens().get(0).getProduto().getNcm()).isEqualTo("69041000");
        assertThat(nota.getInfo().getItens().get(0).getProduto().getCodigoEspecificadorSituacaoTributaria())
                .isEqualTo("1002700");
        assertThat(nota.getInfo().getItens().get(0).getImposto().getIcms().getIcmssn102().getOrigem())
                .isEqualTo(NFOrigem.NACIONAL);
        assertThat(nota.getInfo().getTotal().getIcmsTotal().getValorTotalNFe()).isEqualTo("34.80");
    }

    @Test
    void incluiObservacaoLivreNasInformacoesComplementaresDaNota() {
        NfceXmlFactory factory = new NfceXmlFactory(new StubNfeConfig(DFModelo.NFCE), issuerProperties());

        SaleData sale = new SaleData(
                2001L,
                "2001",
                LocalDateTime.of(2026, 5, 29, 9, 15),
                BigDecimal.ZERO,
                FormaPagamento.DINHEIRO,
                "Entregar mercadoria no caixa lateral.",
                List.of(new SaleItemData(
                        1L,
                        1,
                        new BigDecimal("15.00"),
                        null,
                        new ProductData(10L, "TIJOLO 9X19X19", null, "69041000", null, "NACIONAL", "5102", "UN"))));

        var nota = factory.gerarNotaFiscal(
                sale,
                new RecipientData(
                        "CONSUMIDOR FINAL",
                        "12345678901",
                        null,
                        "75460000",
                        "RUA TESTE",
                        "123",
                        "CENTRO",
                        "5214507",
                        "NEROPOLIS",
                        "GO",
                        null));

        assertThat(nota.getInfo().getInformacoesAdicionais()).isNotNull();
        assertThat(nota.getInfo().getInformacoesAdicionais().getInformacoesComplementaresInteresseContribuinte())
                .isEqualTo("Entregar mercadoria no caixa lateral.");
    }

    @Test
    void rateiaDescontoDoCabecalhoParaOsItensDaNota() {
        NfceXmlFactory factory = new NfceXmlFactory(new StubNfeConfig(DFModelo.NFCE), issuerProperties());

        SaleData sale = new SaleData(
                1005L,
                "1005",
                LocalDateTime.of(2026, 5, 25, 17, 3, 30),
                BigDecimal.TEN,
                FormaPagamento.CARTAO_DEBITO,
                null,
                List.of(new SaleItemData(
                        1L,
                        1,
                        new BigDecimal("100.00"),
                        new BigDecimal("100.00"),
                        new ProductData(10L, "CAMISETA BRANCA", null, "61091000", null, "NACIONAL", "5102", "UN"))));

        var nota = factory.gerarNotaFiscal(
                sale,
                new RecipientData(
                        "CONSUMIDOR FINAL",
                        "12345678901",
                        null,
                        "75460000",
                        "RUA TESTE",
                        "123",
                        "CENTRO",
                        "5214507",
                        "NEROPOLIS",
                        "GO",
                        null));

        assertThat(nota.getInfo().getItens()).hasSize(1);
        assertThat(nota.getInfo().getItens().get(0).getProduto().getValorTotalBruto()).isEqualTo("100.00");
        assertThat(nota.getInfo().getItens().get(0).getProduto().getValorDesconto()).isEqualTo("10.00");
        assertThat(nota.getInfo().getTotal().getIcmsTotal().getValorTotalDesconto()).isEqualTo("10.00");
        assertThat(nota.getInfo().getTotal().getIcmsTotal().getValorTotalNFe()).isEqualTo("90.00");
        assertThat(nota.getInfo().getPagamento().getDetalhamentoFormasPagamento().get(0).getValorPagamento())
                .isEqualTo("90.00");
        assertThat(nota.getInfo().getPagamento().getDetalhamentoFormasPagamento().get(0).getCartao()).isNotNull();
        assertThat(nota.getInfo().getPagamento().getDetalhamentoFormasPagamento().get(0).getCartao().getTipoIntegracao())
                .isEqualTo(NFTipoIntegracaoPagamento.SEPARADO);
    }

    @Test
    void usaSemGtinQuandoCodigoDeBarrasNaoForUmGtinValido() {
        NfceXmlFactory factory = new NfceXmlFactory(new StubNfeConfig(DFModelo.NFCE), issuerProperties());

        SaleData sale = new SaleData(
                1006L,
                "1006",
                LocalDateTime.of(2026, 5, 25, 18, 20, 42),
                BigDecimal.ZERO,
                FormaPagamento.DINHEIRO,
                null,
                List.of(new SaleItemData(
                        1L,
                        1,
                        new BigDecimal("100.00"),
                        null,
                        new ProductData(10L, "CAMISETA PRETA", "00000000000001", "61091000", null, "NACIONAL", "5102", "UN"))));

        var nota = factory.gerarNotaFiscal(
                sale,
                new RecipientData(
                        "CONSUMIDOR FINAL",
                        "12345678901",
                        null,
                        "75460000",
                        "RUA TESTE",
                        "123",
                        "CENTRO",
                        "5214507",
                        "NEROPOLIS",
                        "GO",
                        null));

        assertThat(nota.getInfo().getItens()).hasSize(1);
        assertThat(nota.getInfo().getItens().get(0).getProduto().getCodigoDeBarras()).isEqualTo("SEM GTIN");
        assertThat(nota.getInfo().getItens().get(0).getProduto().getCodigoDeBarrasTributavel()).isEqualTo("SEM GTIN");
    }

    @Test
    void geraNotaComIeDoDestinatarioQuandoInformada() {
        NfceXmlFactory factory = new NfceXmlFactory(new StubNfeConfig(DFModelo.NFE), issuerProperties());

        SaleData sale = new SaleData(
                1002L,
                "1002",
                LocalDateTime.of(2026, 5, 7, 10, 0),
                BigDecimal.ZERO,
                FormaPagamento.DINHEIRO,
                null,
                List.of(new SaleItemData(
                        1L,
                        1,
                        BigDecimal.valueOf(10),
                        null,
                        new ProductData(10L, "BLOCO CERAMICO", null, "69041000", null, "NACIONAL", "5102", "UN"))));

        var nota = factory.gerarNotaFiscal(
                sale,
                new RecipientData(
                        "CLIENTE CONTRIBUINTE",
                        "11222333000144",
                        "109876543",
                        "75460000",
                        "RUA TESTE",
                        "123",
                        "CENTRO",
                        "5214507",
                        "NEROPOLIS",
                        "GO",
                        "financeiro@cliente.com"));

        assertThat(nota.getInfo().getDestinatario().getCnpj()).isEqualTo("11222333000144");
        assertThat(nota.getInfo().getDestinatario().getInscricaoEstadual()).isEqualTo("109876543");
        assertThat(nota.getInfo().getDestinatario().getIndicadorIEDestinatario())
                .isEqualTo(NFIndicadorIEDestinatario.CONTRIBUINTE_ICMS);
    }

    @Test
    void geraNotaInterestadualComIdDestDoisEcfopSeis() {
        NfceXmlFactory factory = new NfceXmlFactory(new StubNfeConfig(DFModelo.NFE), issuerProperties());

        SaleData sale = new SaleData(
                1003L,
                "1003",
                LocalDateTime.of(2026, 5, 7, 11, 0),
                BigDecimal.ZERO,
                FormaPagamento.DINHEIRO,
                null,
                List.of(new SaleItemData(
                        1L,
                        1,
                        BigDecimal.valueOf(10),
                        null,
                        new ProductData(10L, "BLOCO CERAMICO", null, "69041000", null, "NACIONAL", "6102", "UN"))));

        var nota = factory.gerarNotaFiscal(
                sale,
                new RecipientData(
                        "CLIENTE INTERESTADUAL",
                        "11222333000144",
                        "109876543",
                        "70000000",
                        "RUA TESTE",
                        "123",
                        "CENTRO",
                        "5300108",
                        "BRASILIA",
                        "DF",
                        "financeiro@cliente.com"));

        assertThat(nota.getInfo().getIdentificacao().getIdentificadorLocalDestinoOperacao())
                .isEqualTo(NFIdentificadorLocalDestinoOperacao.OPERACAO_INTERESTADUAL);
        assertThat(nota.getInfo().getItens().get(0).getProduto().getCfop()).isEqualTo("6102");
    }

    @Test
    void rejeitaCfopInterestadualQuandoDestinoForInterno() {
        NfceXmlFactory factory = new NfceXmlFactory(new StubNfeConfig(DFModelo.NFE), issuerProperties());

        SaleData sale = new SaleData(
                1004L,
                "1004",
                LocalDateTime.now(),
                BigDecimal.ZERO,
                FormaPagamento.DINHEIRO,
                null,
                List.of(new SaleItemData(
                        1L,
                        1,
                        BigDecimal.TEN,
                        null,
                        new ProductData(10L, "PRODUTO TESTE", null, "69041000", null, "NACIONAL", "6102", "UN"))));

        assertThatThrownBy(() -> factory.gerarNotaFiscal(
                sale,
                new RecipientData("CLIENTE GO", "11222333000144", "109876543", "75460000", "RUA TESTE", "1",
                        "CENTRO", "5214507", "NEROPOLIS", "GO", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CFOP 6102");
    }

    @Test
    void rejeitaDestinatarioSemDocumentoValido() {
        NfceXmlFactory factory = new NfceXmlFactory(new StubNfeConfig(DFModelo.NFE), issuerProperties());

        SaleData sale = new SaleData(
                1001L,
                "1001",
                LocalDateTime.now(),
                BigDecimal.ZERO,
                FormaPagamento.DINHEIRO,
                null,
                List.of(new SaleItemData(
                        1L,
                        1,
                        BigDecimal.TEN,
                        null,
                        new ProductData(10L, "PRODUTO TESTE", null, null, null, null, null, null))));

        assertThatThrownBy(() -> factory.gerarNotaFiscal(
                sale,
                new RecipientData("Murilo Oliveira Rodrigues", "123", "71827615", null, null, null, null, null, null,
                        null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CPF/CNPJ do destinatario invalido");
    }

    @Test
    void exigeDestinatarioQuandoModeloForNfe() {
        NfceXmlFactory factory = new NfceXmlFactory(new StubNfeConfig(DFModelo.NFE), issuerProperties());

        SaleData sale = new SaleData(
                1001L,
                "1001",
                LocalDateTime.now(),
                BigDecimal.ZERO,
                FormaPagamento.DINHEIRO,
                null,
                List.of(new SaleItemData(
                        1L,
                        1,
                        BigDecimal.TEN,
                        null,
                        new ProductData(10L, "PRODUTO TESTE", null, null, null, null, null, null))));

        assertThatThrownBy(() -> factory.gerarNotaFiscal(sale, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("destinatario");
    }

    private NfceIssuerProperties issuerProperties() {
        NfceIssuerProperties properties = new NfceIssuerProperties();
        properties.setCnpj("11222333000144");
        properties.setRazaoSocial("EMPRESA DEMONSTRACAO LTDA");
        properties.setNomeFantasia("LOJA DEMONSTRACAO");
        properties.setInscricaoEstadual("123456789");
        properties.setSerie("1");
        properties.setNaturezaOperacao("VENDA DE MERCADORIA");
        properties.setCep("75460000");
        properties.setLogradouro("AVENIDA EXEMPLO");
        properties.setNumero("0");
        properties.setBairro("BAIRRO EXEMPLO");
        properties.setCodigoMunicipio("5214507");
        properties.setDescricaoMunicipio("NEROPOLIS");
        return properties;
    }

    private static class StubNfeConfig extends NFeConfig {

        private final DFModelo modelo;

        private StubNfeConfig(DFModelo modelo) {
            this.modelo = modelo;
        }

        @Override
        public DFAmbiente getAmbiente() {
            return DFAmbiente.HOMOLOGACAO;
        }

        @Override
        public DFModelo getModelo() {
            return modelo;
        }

        @Override
        public DFUnidadeFederativa getCUF() {
            return DFUnidadeFederativa.GO;
        }

        @Override
        public KeyStore getCertificadoKeyStore() throws KeyStoreException {
            return null;
        }

        @Override
        public String getCertificadoSenha() {
            return "";
        }

        @Override
        public KeyStore getCadeiaCertificadosKeyStore() throws KeyStoreException {
            return null;
        }

        @Override
        public String getCadeiaCertificadosSenha() {
            return "";
        }
    }
}
