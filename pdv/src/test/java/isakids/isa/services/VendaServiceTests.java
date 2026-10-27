package isakids.isa.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import isakids.isa.dto.FiscalNfceResponse;
import isakids.isa.entities.ItemVenda;
import isakids.isa.entities.Loja;
import isakids.isa.entities.Produto;
import isakids.isa.entities.Usuario;
import isakids.isa.entities.Venda;
import isakids.isa.enums.FormaPagamento;
import isakids.isa.enums.StatusVenda;
import isakids.isa.repositories.MovimentacaoEstoqueRepository;
import isakids.isa.repositories.ProdutoLojaRepository;
import isakids.isa.repositories.VendaRepository;

class VendaServiceTests {

    @Test
    void abrirVendaReaproveitaEResetaVendaAbertaInconsistente() {
        VendaRepository vendaRepository = mock(VendaRepository.class);
        FiscalIntegrationService fiscalIntegrationService = mock(FiscalIntegrationService.class);
        NfceDanfeTermicaService nfceDanfeTermicaService = mock(NfceDanfeTermicaService.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);

        Loja loja = new Loja();
        loja.setId(10L);
        Usuario usuario = new Usuario(3L, null, null, null, null, true, loja);
        usuario.setLoja(loja);

        Venda vendaCorrompida = new Venda();
        vendaCorrompida.setId(99L);
        vendaCorrompida.setUsuario(usuario);
        vendaCorrompida.setLoja(loja);
        vendaCorrompida.setStatus(null);
        vendaCorrompida.setFormaPagamento(FormaPagamento.PIX);
        vendaCorrompida.setCpfCnpjConsumidor("12345678901");
        vendaCorrompida.setDesconto(null);
        vendaCorrompida.setTotalBruto(null);
        vendaCorrompida.setTotalLiquido(null);
        vendaCorrompida.setItens(null);

        when(vendaRepository.findIdsByUsuarioIdAndLojaIdAndStatusOrStatusNullOrderByDataHoraDesc(
                3L, 10L, StatusVenda.ABERTA)).thenReturn(List.of(99L));
        when(vendaRepository.findDetalhadaById(99L)).thenReturn(Optional.of(vendaCorrompida));
        when(vendaRepository.save(any(Venda.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VendaService service = buildService(vendaRepository, fiscalIntegrationService, nfceDanfeTermicaService,
                transactionTemplate);

        Venda vendaAberta = service.abrirVenda(usuario);

        assertThat(vendaAberta.getId()).isEqualTo(99L);
        assertThat(vendaAberta.getStatus()).isEqualTo(StatusVenda.ABERTA);
        assertThat(vendaAberta.getFormaPagamento()).isNull();
        assertThat(vendaAberta.getCpfCnpjConsumidor()).isNull();
        assertThat(vendaAberta.getDesconto()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(vendaAberta.getTotalBruto()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(vendaAberta.getTotalLiquido()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(vendaAberta.getItens()).isEmpty();
    }

    @Test
    void emitirNfcePersisteRetornoFiscalNaVenda() {
        VendaRepository vendaRepository = mock(VendaRepository.class);
        FiscalIntegrationService fiscalIntegrationService = mock(FiscalIntegrationService.class);
        NfceDanfeTermicaService nfceDanfeTermicaService = mock(NfceDanfeTermicaService.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);

        Venda venda = vendaConcluida();
        when(vendaRepository.findDetalhadaById(1L)).thenReturn(Optional.of(venda));
        when(vendaRepository.findById(1L)).thenReturn(Optional.of(venda));
        when(vendaRepository.save(any(Venda.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<Venda> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        FiscalNfceResponse response = new FiscalNfceResponse();
        response.setEmpresaId(2L);
        response.setEmpresaNome("Isakids e Teens");
        response.setModelo("NFCE");
        response.setCStat(100);
        response.setMotivo("Autorizado o uso da NF-e");
        response.setChaveAcesso("52260511222333000144650010000000551000000055");
        response.setProtocoloAutorizacao("123456789012345");
        response.setQrCodeUrl("https://sefaz/go/qr");
        when(fiscalIntegrationService.emitirNfce(venda, "12345678901")).thenReturn(response);

        VendaService service = buildService(vendaRepository, fiscalIntegrationService, nfceDanfeTermicaService,
                transactionTemplate);

        Venda vendaEmitida = service.emitirNfce(1L, "123.456.789-01");

        assertThat(vendaEmitida.getNotaFiscalEmpresaId()).isEqualTo(2L);
        assertThat(vendaEmitida.getNotaFiscalEmpresaNome()).isEqualTo("Isakids e Teens");
        assertThat(vendaEmitida.getNotaFiscalModelo()).isEqualTo("NFCE");
        assertThat(vendaEmitida.getNotaFiscalSituacao()).isEqualTo("AUTORIZADA");
        assertThat(vendaEmitida.getNotaFiscalChaveAcesso()).isEqualTo("52260511222333000144650010000000551000000055");
        assertThat(vendaEmitida.getCpfCnpjConsumidor()).isEqualTo("12345678901");
        assertThat(vendaEmitida.getNfcePdfDownloadUrl()).isEqualTo("/api/pdv/vendas/1/nfce/pdf");
        assertThat(vendaEmitida.getNfcePdfTermicoDownloadUrl()).isEqualTo("/api/pdv/vendas/1/nfce/pdf-termico");
    }

    @Test
    void emitirNfcePermiteEscolherSemCpfMesmoComDocumentoNaVenda() {
        VendaRepository vendaRepository = mock(VendaRepository.class);
        FiscalIntegrationService fiscalIntegrationService = mock(FiscalIntegrationService.class);
        NfceDanfeTermicaService nfceDanfeTermicaService = mock(NfceDanfeTermicaService.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);

        Venda venda = vendaConcluida();
        venda.setCpfCnpjConsumidor("12345678901");
        when(vendaRepository.findDetalhadaById(1L)).thenReturn(Optional.of(venda));
        when(vendaRepository.findById(1L)).thenReturn(Optional.of(venda));
        when(vendaRepository.save(any(Venda.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<Venda> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        FiscalNfceResponse response = new FiscalNfceResponse();
        response.setEmpresaId(2L);
        response.setEmpresaNome("Isakids e Teens");
        response.setModelo("NFCE");
        response.setCStat(100);
        response.setChaveAcesso("52260511222333000144650010000000551000000055");
        when(fiscalIntegrationService.emitirNfce(venda, null)).thenReturn(response);

        VendaService service = buildService(vendaRepository, fiscalIntegrationService, nfceDanfeTermicaService,
                transactionTemplate);

        Venda vendaEmitida = service.emitirNfce(1L, null, false);

        assertThat(vendaEmitida.getCpfCnpjConsumidor()).isNull();
        verify(fiscalIntegrationService).emitirNfce(venda, null);
    }

    @Test
    void emitirNfcePermiteReemissaoQuandoUltimoRetornoFiscalFoiErro() {
        VendaRepository vendaRepository = mock(VendaRepository.class);
        FiscalIntegrationService fiscalIntegrationService = mock(FiscalIntegrationService.class);
        NfceDanfeTermicaService nfceDanfeTermicaService = mock(NfceDanfeTermicaService.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);

        Venda venda = vendaConcluida();
        venda.setNotaFiscalEmpresaId(10L);
        venda.setNotaFiscalSituacao("ERRO");
        venda.setNotaFiscalCStat(537);
        venda.setNotaFiscalChaveAcesso("53260511222333000144650010000000041771698643");
        when(vendaRepository.findDetalhadaById(1L)).thenReturn(Optional.of(venda));
        when(vendaRepository.findById(1L)).thenReturn(Optional.of(venda));
        when(vendaRepository.save(any(Venda.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<Venda> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        FiscalNfceResponse response = new FiscalNfceResponse();
        response.setEmpresaId(2L);
        response.setEmpresaNome("Isakids e Teens");
        response.setModelo("NFCE");
        response.setCStat(100);
        response.setMotivo("Autorizado o uso da NF-e");
        response.setChaveAcesso("52260511222333000144650010000000551000000055");
        response.setProtocoloAutorizacao("123456789012345");
        when(fiscalIntegrationService.emitirNfce(venda, null)).thenReturn(response);

        VendaService service = buildService(vendaRepository, fiscalIntegrationService, nfceDanfeTermicaService,
                transactionTemplate);

        Venda vendaEmitida = service.emitirNfce(1L, null);

        assertThat(vendaEmitida.getNotaFiscalSituacao()).isEqualTo("AUTORIZADA");
        assertThat(vendaEmitida.getNotaFiscalCStat()).isEqualTo(100);
        assertThat(vendaEmitida.getNotaFiscalChaveAcesso()).isEqualTo("52260511222333000144650010000000551000000055");
        verify(fiscalIntegrationService).emitirNfce(venda, null);
    }

    @Test
    void emitirNfceNaoReemiteQuandoVendaJaTemNotaAutorizada() {
        VendaRepository vendaRepository = mock(VendaRepository.class);
        FiscalIntegrationService fiscalIntegrationService = mock(FiscalIntegrationService.class);
        NfceDanfeTermicaService nfceDanfeTermicaService = mock(NfceDanfeTermicaService.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);

        Venda venda = vendaConcluida();
        venda.setNotaFiscalEmpresaId(10L);
        venda.setNotaFiscalSituacao("AUTORIZADA");
        venda.setNotaFiscalCStat(100);
        venda.setNotaFiscalChaveAcesso("52260511222333000144650010000000551000000055");
        when(vendaRepository.findDetalhadaById(1L)).thenReturn(Optional.of(venda));

        VendaService service = buildService(vendaRepository, fiscalIntegrationService, nfceDanfeTermicaService,
                transactionTemplate);

        Venda vendaEmitida = service.emitirNfce(1L, null);

        assertThat(vendaEmitida).isSameAs(venda);
        verify(fiscalIntegrationService, never()).emitirNfce(any(Venda.class), any());
    }

    @Test
    void emitirNfceNaoReemiteQuandoSituacaoPersistidaEstaDesconhecidaMasCStatJaAutorizou() {
        VendaRepository vendaRepository = mock(VendaRepository.class);
        FiscalIntegrationService fiscalIntegrationService = mock(FiscalIntegrationService.class);
        NfceDanfeTermicaService nfceDanfeTermicaService = mock(NfceDanfeTermicaService.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);

        Venda venda = vendaConcluida();
        venda.setNotaFiscalEmpresaId(10L);
        venda.setNotaFiscalSituacao("DESCONHECIDA");
        venda.setNotaFiscalCStat(100);
        venda.setNotaFiscalChaveAcesso("52260511222333000144650010000000551000000055");
        when(vendaRepository.findDetalhadaById(1L)).thenReturn(Optional.of(venda));

        VendaService service = buildService(vendaRepository, fiscalIntegrationService, nfceDanfeTermicaService,
                transactionTemplate);

        Venda vendaEmitida = service.emitirNfce(1L, null);

        assertThat(vendaEmitida.getNotaFiscalSituacao()).isEqualTo("AUTORIZADA");
        verify(fiscalIntegrationService, never()).emitirNfce(any(Venda.class), any());
    }

    @Test
    void emitirNfceNaoReemiteQuandoRetornoLegadoTemProtocoloEMotivoDeAutorizacao() {
        VendaRepository vendaRepository = mock(VendaRepository.class);
        FiscalIntegrationService fiscalIntegrationService = mock(FiscalIntegrationService.class);
        NfceDanfeTermicaService nfceDanfeTermicaService = mock(NfceDanfeTermicaService.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);

        Venda venda = vendaConcluida();
        venda.setNotaFiscalEmpresaId(33L);
        venda.setNotaFiscalSituacao("DESCONHECIDA");
        venda.setNotaFiscalCStat(null);
        venda.setNotaFiscalChaveAcesso("53260511222333000144650010000000011610097093");
        venda.setNotaFiscalProtocoloAutorizacao("123456789012345");
        venda.setNotaFiscalMotivo("Autorizado o uso da NF-e");
        when(vendaRepository.findDetalhadaById(1L)).thenReturn(Optional.of(venda));

        VendaService service = buildService(vendaRepository, fiscalIntegrationService, nfceDanfeTermicaService,
                transactionTemplate);

        Venda vendaEmitida = service.emitirNfce(1L, null);

        assertThat(vendaEmitida.getNotaFiscalSituacao()).isEqualTo("AUTORIZADA");
        verify(fiscalIntegrationService, never()).emitirNfce(any(Venda.class), any());
    }

    @Test
    void emitirNfceComCpfExplicitoExigeDocumentoQuandoNaoHaCpfPersistido() {
        VendaRepository vendaRepository = mock(VendaRepository.class);
        FiscalIntegrationService fiscalIntegrationService = mock(FiscalIntegrationService.class);
        NfceDanfeTermicaService nfceDanfeTermicaService = mock(NfceDanfeTermicaService.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);

        Venda venda = vendaConcluida();
        when(vendaRepository.findDetalhadaById(1L)).thenReturn(Optional.of(venda));

        VendaService service = buildService(vendaRepository, fiscalIntegrationService, nfceDanfeTermicaService,
                transactionTemplate);

        assertThatThrownBy(() -> service.emitirNfce(1L, null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("emitir a NFC-e com CPF/CNPJ");
        verify(fiscalIntegrationService, never()).emitirNfce(any(Venda.class), any());
    }

    @Test
    void emitirNfceExigeProdutoComDadosFiscais() {
        VendaRepository vendaRepository = mock(VendaRepository.class);
        FiscalIntegrationService fiscalIntegrationService = mock(FiscalIntegrationService.class);
        NfceDanfeTermicaService nfceDanfeTermicaService = mock(NfceDanfeTermicaService.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);

        Venda venda = vendaConcluida();
        venda.getItens().get(0).getProduto().setNcm(null);
        when(vendaRepository.findDetalhadaById(1L)).thenReturn(Optional.of(venda));

        VendaService service = buildService(vendaRepository, fiscalIntegrationService, nfceDanfeTermicaService,
                transactionTemplate);

        assertThatThrownBy(() -> service.emitirNfce(1L, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("configuracao fiscal incompleta")
                .hasMessageContaining("ncm");
        verify(fiscalIntegrationService, never()).emitirNfce(any(Venda.class), any());
    }

    @Test
    void finalizarVendaExigeItensAntesDeConcluir() {
        VendaRepository vendaRepository = mock(VendaRepository.class);
        FiscalIntegrationService fiscalIntegrationService = mock(FiscalIntegrationService.class);
        NfceDanfeTermicaService nfceDanfeTermicaService = mock(NfceDanfeTermicaService.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);

        Venda venda = new Venda();
        venda.setId(1L);
        venda.setLoja(new Loja());
        venda.setUsuario(new Usuario());
        venda.setStatus(StatusVenda.ABERTA);
        venda.setItens(List.of());

        when(vendaRepository.findDetalhadaById(1L)).thenReturn(Optional.of(venda));

        VendaService service = buildService(vendaRepository, fiscalIntegrationService, nfceDanfeTermicaService,
                transactionTemplate);

        assertThatThrownBy(() -> service.finalizarVenda(1L, FormaPagamento.PIX, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("precisa ter itens");
    }

    @Test
    void atualizarIgnoraCamposNulosParaNaoCorromperVenda() {
        VendaRepository vendaRepository = mock(VendaRepository.class);
        FiscalIntegrationService fiscalIntegrationService = mock(FiscalIntegrationService.class);
        NfceDanfeTermicaService nfceDanfeTermicaService = mock(NfceDanfeTermicaService.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);

        Loja loja = new Loja();
        loja.setId(10L);
        Usuario usuario = new Usuario(5L, null, null, null, null, true, loja);

        Venda existente = new Venda();
        existente.setId(1L);
        existente.setUsuario(usuario);
        existente.setLoja(loja);
        existente.setStatus(StatusVenda.ABERTA);
        existente.setFormaPagamento(FormaPagamento.PIX);
        existente.setDesconto(new BigDecimal("3.00"));

        when(vendaRepository.getReferenceById(1L)).thenReturn(existente);
        when(vendaRepository.save(eq(existente))).thenReturn(existente);

        VendaService service = buildService(vendaRepository, fiscalIntegrationService, nfceDanfeTermicaService,
                transactionTemplate);

        Venda atualizado = new Venda();
        atualizado.setLoja(null);
        atualizado.setUsuario(null);
        atualizado.setStatus(null);
        atualizado.setFormaPagamento(null);
        atualizado.setDesconto(null);

        Venda resposta = service.atualizar(1L, atualizado);

        assertThat(resposta.getUsuario()).isSameAs(usuario);
        assertThat(resposta.getLoja()).isSameAs(loja);
        assertThat(resposta.getStatus()).isEqualTo(StatusVenda.ABERTA);
        assertThat(resposta.getFormaPagamento()).isEqualTo(FormaPagamento.PIX);
        assertThat(resposta.getDesconto()).isEqualByComparingTo("3.00");
    }

    @Test
    void carregarPdfNfceUsaReferenciaDaVendaSemExigirChaveDeAcesso() {
        VendaRepository vendaRepository = mock(VendaRepository.class);
        FiscalIntegrationService fiscalIntegrationService = mock(FiscalIntegrationService.class);
        NfceDanfeTermicaService nfceDanfeTermicaService = mock(NfceDanfeTermicaService.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);

        Venda venda = vendaConcluida();
        venda.setNotaFiscalEmpresaId(99L);
        venda.setNotaFiscalChaveAcesso(null);
        byte[] pdf = "%PDF".getBytes();

        when(vendaRepository.findById(1L)).thenReturn(Optional.of(venda));
        when(fiscalIntegrationService.baixarPdf(venda)).thenReturn(pdf);

        VendaService service = buildService(vendaRepository, fiscalIntegrationService, nfceDanfeTermicaService,
                transactionTemplate);

        assertThat(service.carregarPdfNfce(1L)).isEqualTo(pdf);
        verify(fiscalIntegrationService).baixarPdf(venda);
    }

    @Test
    void carregarPdfNfceTermicoGeraDanfeLocalComXmlDaVenda() {
        VendaRepository vendaRepository = mock(VendaRepository.class);
        FiscalIntegrationService fiscalIntegrationService = mock(FiscalIntegrationService.class);
        NfceDanfeTermicaService nfceDanfeTermicaService = mock(NfceDanfeTermicaService.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);

        Venda venda = vendaConcluida();
        venda.setNotaFiscalEmpresaId(99L);
        byte[] xml = "<xml/>".getBytes();
        byte[] pdf = "%PDF-TERMICO".getBytes();

        when(vendaRepository.findDetalhadaById(1L)).thenReturn(Optional.of(venda));
        when(fiscalIntegrationService.baixarXml(venda)).thenReturn(xml);
        when(nfceDanfeTermicaService.gerarDanfe(venda, xml)).thenReturn(pdf);

        VendaService service = buildService(vendaRepository, fiscalIntegrationService, nfceDanfeTermicaService,
                transactionTemplate);

        assertThat(service.carregarPdfNfceTermico(1L)).isEqualTo(pdf);
        verify(fiscalIntegrationService).baixarXml(venda);
        verify(nfceDanfeTermicaService).gerarDanfe(venda, xml);
    }

    private VendaService buildService(VendaRepository vendaRepository,
            FiscalIntegrationService fiscalIntegrationService,
            NfceDanfeTermicaService nfceDanfeTermicaService,
            TransactionTemplate transactionTemplate) {
        VendaService service = new VendaService();
        service.vendaRepository = vendaRepository;
        service.produtoLojaRepository = mock(ProdutoLojaRepository.class);
        service.movimentacaoEstoqueRepository = mock(MovimentacaoEstoqueRepository.class);
        service.authenticatedUserService = mock(AuthenticatedUserService.class);
        service.fiscalIntegrationService = fiscalIntegrationService;
        service.nfceDanfeTermicaService = nfceDanfeTermicaService;
        service.transactionTemplate = transactionTemplate;
        return service;
    }

    private Venda vendaConcluida() {
        Loja loja = new Loja();
        loja.setId(10L);
        loja.setCnpj("11222333000144");
        loja.setNome("Loja Centro");

        Produto produto = new Produto();
        produto.setId(20L);
        produto.setDescricao("Vestido Infantil");
        produto.setCodigoBarras("7891234567890");
        produto.setNcm("62044300");
        produto.setCest("2804700");
        produto.setOrigem("NACIONAL");
        produto.setCfop("5102");
        produto.setUnidadeComercial("UN");
        produto.setPrecoVenda(new BigDecimal("49.90"));

        Venda venda = new Venda();
        venda.setId(1L);
        venda.setLoja(loja);
        venda.setUsuario(new Usuario());
        venda.setStatus(StatusVenda.CONCLUIDA);
        venda.setFormaPagamento(FormaPagamento.PIX);
        venda.setDesconto(BigDecimal.ZERO);

        ItemVenda item = new ItemVenda(venda, produto, 2, new BigDecimal("49.90"));
        venda.setItens(List.of(item));
        venda.recalcularTotais();
        return venda;
    }
}
