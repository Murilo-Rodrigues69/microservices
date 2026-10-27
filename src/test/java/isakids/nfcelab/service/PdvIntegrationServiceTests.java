package isakids.nfcelab.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Transactional;

import isakids.nfcelab.document.NfceDocumentService;
import isakids.nfcelab.dto.EmitirNfceRequest;
import isakids.nfcelab.dto.NfceOperationResponse;
import isakids.nfcelab.dto.PdvNfceIntegrationItemRequest;
import isakids.nfcelab.dto.PdvNfceIntegrationRequest;
import isakids.nfcelab.entity.EmpresaEntity;
import isakids.nfcelab.entity.FiscalAmbiente;
import isakids.nfcelab.entity.FiscalModelo;
import isakids.nfcelab.entity.NotaFiscalEmpresaEntity;
import isakids.nfcelab.entity.SituacaoNotaFiscal;
import isakids.nfcelab.model.FormaPagamento;
import isakids.nfcelab.repository.EmpresaRepository;
import isakids.nfcelab.repository.NotaFiscalEmpresaRepository;

class PdvIntegrationServiceTests {

    @Test
    void methodsThatPersistOrBackfillDocumentsMustRunInWritableTransactions() throws Exception {
        assertWritableTransactionalMethod("emitirNfce", PdvNfceIntegrationRequest.class);
        assertWritableTransactionalMethod("loadPdf", String.class, Long.class);
        assertWritableTransactionalMethod("loadXml", String.class, Long.class);
    }

    @Test
    void emiteNfceMapeandoEmpresaEProdutoDaVenda() {
        EmpresaRepository empresaRepository = mock(EmpresaRepository.class);
        NotaFiscalEmpresaRepository notaFiscalEmpresaRepository = mock(NotaFiscalEmpresaRepository.class);
        NfceLabService nfceLabService = mock(NfceLabService.class);
        NfceDocumentService documentService = mock(NfceDocumentService.class);
        PdvIntegrationService service = new PdvIntegrationService(
                empresaRepository,
                notaFiscalEmpresaRepository,
                nfceLabService,
                documentService);

        EmpresaEntity empresa = empresa("11222333000144", FiscalModelo.NFCE);
        when(empresaRepository.findByCnpjAndAtivoTrue("11222333000144")).thenReturn(Optional.of(empresa));
        when(notaFiscalEmpresaRepository.findByEmpresaIdAndOrigemSistemaAndVendaOrigemId(1L, "ISA", 55L))
                .thenReturn(Optional.empty());

        NfceOperationResponse emissao = new NfceOperationResponse();
        emissao.setChaveAcesso("52260511222333000144650010000000551000000055");
        when(nfceLabService.emitir(any())).thenReturn(emissao);

        NfceOperationResponse response = service.emitirNfce(request());
        ArgumentCaptor<EmitirNfceRequest> requestCaptor = ArgumentCaptor.forClass(EmitirNfceRequest.class);

        assertThat(response.getChaveAcesso()).isEqualTo("52260511222333000144650010000000551000000055");
        verify(nfceLabService).emitir(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getDesconto()).isEqualByComparingTo("9.80");
        assertThat(requestCaptor.getValue().getObservacao()).isEqualTo("Embalar para presente.");
        assertThat(requestCaptor.getValue().getItens()).hasSize(1);
        assertThat(requestCaptor.getValue().getItens().get(0).getDesconto()).isEqualByComparingTo("9.80");
    }

    @Test
    void reutilizaNotaJaPersistidaParaMesmaVendaOrigem() {
        EmpresaRepository empresaRepository = mock(EmpresaRepository.class);
        NotaFiscalEmpresaRepository notaFiscalEmpresaRepository = mock(NotaFiscalEmpresaRepository.class);
        NfceLabService nfceLabService = mock(NfceLabService.class);
        NfceDocumentService documentService = mock(NfceDocumentService.class);
        PdvIntegrationService service = new PdvIntegrationService(
                empresaRepository,
                notaFiscalEmpresaRepository,
                nfceLabService,
                documentService);

        EmpresaEntity empresa = empresa("11222333000144", FiscalModelo.NFCE);
        NotaFiscalEmpresaEntity nota = new NotaFiscalEmpresaEntity();
        nota.setEmpresa(empresa);
        nota.setModelo(FiscalModelo.NFCE);
        nota.setSituacao(SituacaoNotaFiscal.AUTORIZADA);
        nota.setChaveAcesso("52260511222333000144650010000000551000000055");
        nota.setcStat(100);
        nota.setMotivo("Autorizado o uso da NF-e");
        nota.setObservacao("Embalar para presente.");

        when(empresaRepository.findByCnpjAndAtivoTrue("11222333000144")).thenReturn(Optional.of(empresa));
        when(notaFiscalEmpresaRepository.findByEmpresaIdAndOrigemSistemaAndVendaOrigemId(1L, "ISA", 55L))
                .thenReturn(Optional.of(nota));

        NfceOperationResponse response = service.emitirNfce(request());

        assertThat(response.getChaveAcesso()).isEqualTo("52260511222333000144650010000000551000000055");
        assertThat(response.getPdfDownloadUrl()).isEqualTo(
                "/api/nfce/documentos/52260511222333000144650010000000551000000055/pdf");
        assertThat(response.getObservacao()).isEqualTo("Embalar para presente.");
        verify(nfceLabService, never()).emitir(any());
    }

    @Test
    void reemiteQuandoNotaExistenteDaVendaEstaEmErro() {
        EmpresaRepository empresaRepository = mock(EmpresaRepository.class);
        NotaFiscalEmpresaRepository notaFiscalEmpresaRepository = mock(NotaFiscalEmpresaRepository.class);
        NfceLabService nfceLabService = mock(NfceLabService.class);
        NfceDocumentService documentService = mock(NfceDocumentService.class);
        PdvIntegrationService service = new PdvIntegrationService(
                empresaRepository,
                notaFiscalEmpresaRepository,
                nfceLabService,
                documentService);

        EmpresaEntity empresa = empresa("11222333000144", FiscalModelo.NFCE);
        NotaFiscalEmpresaEntity nota = new NotaFiscalEmpresaEntity();
        nota.setEmpresa(empresa);
        nota.setModelo(FiscalModelo.NFCE);
        nota.setSituacao(SituacaoNotaFiscal.ERRO);
        nota.setChaveAcesso("53260530415093000186650010000000041771698643");
        nota.setcStat(537);
        nota.setMotivo("Rejeicao: Total do Desconto difere do somatorio dos itens");

        when(empresaRepository.findByCnpjAndAtivoTrue("11222333000144")).thenReturn(Optional.of(empresa));
        when(notaFiscalEmpresaRepository.findByEmpresaIdAndOrigemSistemaAndVendaOrigemId(1L, "ISA", 55L))
                .thenReturn(Optional.of(nota));

        NfceOperationResponse emissao = new NfceOperationResponse();
        emissao.setChaveAcesso("52260511222333000144650010000000551000000055");
        when(nfceLabService.emitir(any())).thenReturn(emissao);

        NfceOperationResponse response = service.emitirNfce(request());

        assertThat(response.getChaveAcesso()).isEqualTo("52260511222333000144650010000000551000000055");
        verify(nfceLabService).emitir(any());
    }

    private PdvNfceIntegrationRequest request() {
        PdvNfceIntegrationItemRequest item = new PdvNfceIntegrationItemRequest();
        item.setItemId(10L);
        item.setProdutoId(20L);
        item.setDescricao("Vestido Infantil");
        item.setCodigoBarras("7891234567890");
        item.setNcm("62044300");
        item.setCest("2804700");
        item.setOrigem("0");
        item.setCfop("5102");
        item.setUnidadeComercial("UN");
        item.setQuantidade(2);
        item.setPrecoUnitario(new BigDecimal("49.90"));
        item.setSubtotal(new BigDecimal("99.80"));
        item.setDesconto(new BigDecimal("9.80"));

        PdvNfceIntegrationRequest request = new PdvNfceIntegrationRequest();
        request.setLojaCnpj("11.222.333/0001-44");
        request.setVendaId(55L);
        request.setDesconto(new BigDecimal("9.80"));
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setObservacao("Embalar para presente.");
        request.setItens(List.of(item));
        return request;
    }

    private EmpresaEntity empresa(String cnpj, FiscalModelo modelo) {
        EmpresaEntity empresa = new EmpresaEntity();
        empresa.setId(1L);
        empresa.setNome("Isakids");
        empresa.setCnpj(cnpj);
        empresa.setModelo(modelo);
        empresa.setAmbiente(FiscalAmbiente.PRODUCAO);
        empresa.setAtivo(true);
        return empresa;
    }

    private void assertWritableTransactionalMethod(String methodName, Class<?>... parameterTypes) throws Exception {
        Method method = PdvIntegrationService.class.getMethod(methodName, parameterTypes);
        Transactional annotation = method.getAnnotation(Transactional.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.readOnly()).isFalse();
    }
}
