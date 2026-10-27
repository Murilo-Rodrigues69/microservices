package isakids.nfcelab.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import isakids.nfcelab.config.NfceDocumentProperties;
import isakids.nfcelab.document.MeuDanfeClient;
import isakids.nfcelab.document.NfceDocumentService;
import isakids.nfcelab.dto.ConsultarNfceRequest;
import isakids.nfcelab.dto.EmitirNfceRequest;
import isakids.nfcelab.dto.NfceItemRequest;
import isakids.nfcelab.dto.NfceOperationResponse;
import isakids.nfcelab.entity.EmpresaEntity;
import isakids.nfcelab.entity.FiscalAmbiente;
import isakids.nfcelab.entity.FiscalModelo;
import isakids.nfcelab.fiscal.FiscalContextFactory;
import isakids.nfcelab.fiscal.NfceEmissionResult;
import isakids.nfcelab.fiscal.NfceEmitter;
import isakids.nfcelab.model.FormaPagamento;
import isakids.nfcelab.model.ProductData;
import isakids.nfcelab.model.RecipientData;
import isakids.nfcelab.model.SaleData;
import isakids.nfcelab.support.SampleAuthorizedXmls;
import tools.jackson.databind.ObjectMapper;

class NfceLabServiceTests {

    @TempDir
    Path tempDir;

    @Test
    void consultarComModeloNfeRetornaLinksNoPrefixoApiNfe() {
        String chaveAcesso = "52260411222333000144550010000020011000020010";
        EmpresaEntity empresa = empresa(1L, FiscalModelo.NFE);
        NfceEmitter emitter = mock(NfceEmitter.class);
        EmpresaService empresaService = mock(EmpresaService.class);
        NotaFiscalEmpresaService notaFiscalEmpresaService = mock(NotaFiscalEmpresaService.class);
        when(empresaService.resolveEmpresaAtiva(empresa.getId())).thenReturn(empresa);
        when(emitter.consultarPorChave(empresa, chaveAcesso))
                .thenReturn(NfceEmissionResult.builder()
                        .cStat(100)
                        .motivo("Autorizado")
                        .chaveAcesso(chaveAcesso)
                        .build());

        NfceLabService service = newService(emitter, empresaService, mock(ProdutoEmpresaService.class),
                mock(ClienteEmpresaService.class), notaFiscalEmpresaService);
        ConsultarNfceRequest request = new ConsultarNfceRequest();
        request.setEmpresaId(empresa.getId());
        request.setChaveAcesso(chaveAcesso);

        NfceOperationResponse response = service.consultar(request);

        assertThat(response.getXmlDownloadUrl()).isEqualTo("/api/nfe/documentos/" + chaveAcesso + "/xml");
        assertThat(response.getPdfDownloadUrl()).isEqualTo("/api/nfe/documentos/" + chaveAcesso + "/pdf");
        assertThat(response.getEmpresaId()).isEqualTo(empresa.getId());
        assertThat(response.getModelo()).isEqualTo("NFE");
        verify(notaFiscalEmpresaService).registrarConsulta(eq(empresa), eq(request), eq(response));
    }

    @Test
    void consultarComModeloNfceRetornaLinksNoPrefixoApiNfce() {
        String chaveAcesso = "52260411222333000144550010000020011000020010";
        EmpresaEntity empresa = empresa(2L, FiscalModelo.NFCE);
        NfceEmitter emitter = mock(NfceEmitter.class);
        EmpresaService empresaService = mock(EmpresaService.class);
        NotaFiscalEmpresaService notaFiscalEmpresaService = mock(NotaFiscalEmpresaService.class);
        when(empresaService.resolveEmpresaAtiva(empresa.getId())).thenReturn(empresa);
        when(emitter.consultarPorChave(empresa, chaveAcesso))
                .thenReturn(NfceEmissionResult.builder()
                        .cStat(100)
                        .motivo("Autorizado")
                        .chaveAcesso(chaveAcesso)
                        .build());

        NfceLabService service = newService(emitter, empresaService, mock(ProdutoEmpresaService.class),
                mock(ClienteEmpresaService.class), notaFiscalEmpresaService);
        ConsultarNfceRequest request = new ConsultarNfceRequest();
        request.setEmpresaId(empresa.getId());
        request.setChaveAcesso(chaveAcesso);

        NfceOperationResponse response = service.consultar(request);

        assertThat(response.getXmlDownloadUrl()).isEqualTo("/api/nfce/documentos/" + chaveAcesso + "/xml");
        assertThat(response.getPdfDownloadUrl()).isEqualTo("/api/nfce/documentos/" + chaveAcesso + "/pdf");
        assertThat(response.getEmpresaId()).isEqualTo(empresa.getId());
        assertThat(response.getModelo()).isEqualTo("NFCE");
        verify(notaFiscalEmpresaService).registrarConsulta(eq(empresa), eq(request), eq(response));
    }

    @Test
    void emitirResolveProdutoEClienteDaEmpresa() {
        String chaveAcesso = "52260411222333000144550010000020011000020010";
        EmpresaEntity empresa = empresa(1L, FiscalModelo.NFE);
        EmpresaService empresaService = mock(EmpresaService.class);
        ProdutoEmpresaService produtoEmpresaService = mock(ProdutoEmpresaService.class);
        ClienteEmpresaService clienteEmpresaService = mock(ClienteEmpresaService.class);
        NotaFiscalEmpresaService notaFiscalEmpresaService = mock(NotaFiscalEmpresaService.class);
        NfceEmitter emitter = mock(NfceEmitter.class);

        when(empresaService.resolveEmpresaAtiva(empresa.getId())).thenReturn(empresa);
        when(produtoEmpresaService.toProductData(empresa.getId(), 9L))
                .thenReturn(new ProductData(9L, "TIJOLO 6 FUROS", "7891234567890", "69041000", "1002700",
                        "NACIONAL", "5102", "UN"));
        when(clienteEmpresaService.toRecipientData(empresa.getId(), 7L))
                .thenReturn(new RecipientData("CLIENTE DB", "12345678901", null, "75460000", "RUA A", "10",
                        "CENTRO", "5214507", "NEROPOLIS", "GO", "cliente@teste.com"));
        when(emitter.emitir(eq(empresa), any(SaleData.class), any(RecipientData.class)))
                .thenReturn(NfceEmissionResult.builder()
                        .cStat(100)
                        .motivo("Autorizado")
                        .chaveAcesso(chaveAcesso)
                        .xmlAutorizado(SampleAuthorizedXmls.authorizedNfe())
                        .build());

        NfceLabService service = newService(emitter, empresaService, produtoEmpresaService, clienteEmpresaService,
                notaFiscalEmpresaService);

        EmitirNfceRequest request = new EmitirNfceRequest();
        request.setEmpresaId(empresa.getId());
        request.setClienteEmpresaId(7L);
        request.setNumeroNota("3001");
        request.setFormaPagamento(FormaPagamento.DINHEIRO);
        request.setObservacao("Retirar no caixa 2.");

        NfceItemRequest item = new NfceItemRequest();
        item.setItemId(1L);
        item.setProdutoEmpresaId(9L);
        item.setQuantidade(10);
        item.setPrecoUnitario(BigDecimal.valueOf(1.25));
        request.setItens(java.util.List.of(item));

        NfceOperationResponse response = service.emitir(request);

        ArgumentCaptor<SaleData> saleCaptor = ArgumentCaptor.forClass(SaleData.class);
        ArgumentCaptor<RecipientData> recipientCaptor = ArgumentCaptor.forClass(RecipientData.class);
        verify(emitter).emitir(eq(empresa), saleCaptor.capture(), recipientCaptor.capture());

        assertThat(saleCaptor.getValue().itens()).hasSize(1);
        assertThat(saleCaptor.getValue().itens().get(0).produto().descricao()).isEqualTo("TIJOLO 6 FUROS");
        assertThat(saleCaptor.getValue().observacao()).isEqualTo("Retirar no caixa 2.");
        assertThat(recipientCaptor.getValue().nome()).isEqualTo("CLIENTE DB");
        assertThat(response.getEmpresaId()).isEqualTo(empresa.getId());
        assertThat(response.getModelo()).isEqualTo("NFE");
        assertThat(response.getObservacao()).isEqualTo("Retirar no caixa 2.");
        assertThat(response.getXmlDownloadUrl()).isEqualTo("/api/nfe/documentos/" + chaveAcesso + "/xml");
        verify(notaFiscalEmpresaService).registrarEmissao(eq(empresa), eq(request), any(SaleData.class),
                any(RecipientData.class), eq(response));
    }

    private NfceLabService newService(NfceEmitter emitter, EmpresaService empresaService) {
        return newService(emitter, empresaService, mock(ProdutoEmpresaService.class), mock(ClienteEmpresaService.class),
                mock(NotaFiscalEmpresaService.class));
    }

    private NfceLabService newService(NfceEmitter emitter, EmpresaService empresaService,
            ProdutoEmpresaService produtoEmpresaService, ClienteEmpresaService clienteEmpresaService,
            NotaFiscalEmpresaService notaFiscalEmpresaService) {
        NfceDocumentProperties properties = new NfceDocumentProperties();
        properties.setOutputDir(tempDir.toString());

        NfceDocumentService documentService = new NfceDocumentService(
                properties,
                new isakids.nfcelab.document.NfcePdfRenderer(),
                new isakids.nfcelab.document.NfceDanfeRenderer(),
                new MeuDanfeClient(properties, new ObjectMapper()));
        documentService.store("52260411222333000144550010000020011000020010", SampleAuthorizedXmls.authorizedNfe());

        return new NfceLabService(
                emitter,
                mock(FiscalContextFactory.class),
                empresaService,
                produtoEmpresaService,
                clienteEmpresaService,
                documentService,
                notaFiscalEmpresaService,
                new DocumentoFiscalXmlParser());
    }

    private EmpresaEntity empresa(Long id, FiscalModelo modelo) {
        EmpresaEntity empresa = new EmpresaEntity();
        empresa.setId(id);
        empresa.setNome("Empresa " + id);
        empresa.setCnpj("11222333000144");
        empresa.setRazaoSocial("EMPRESA DEMONSTRACAO LTDA");
        empresa.setNomeFantasia("CERAMICA MASTER");
        empresa.setInscricaoEstadual("201659450");
        empresa.setSerie("1");
        empresa.setNaturezaOperacao("VENDA DE MERCADORIA");
        empresa.setCep("75460000");
        empresa.setLogradouro("AVENIDA EXEMPLO");
        empresa.setNumero("0");
        empresa.setBairro("BAIRRO EXEMPLO");
        empresa.setCodigoMunicipio("5214507");
        empresa.setDescricaoMunicipio("NEROPOLIS");
        empresa.setUf("GO");
        empresa.setModelo(modelo);
        empresa.setAmbiente(FiscalAmbiente.PRODUCAO);
        empresa.setAtivo(true);
        empresa.setCertificadoPfxPath("C:/certificado.pfx");
        empresa.setCertificadoSenha("123");
        empresa.setCadeiaJksPath("C:/cadeia.jks");
        empresa.setCadeiaSenha("123");
        return empresa;
    }
}
