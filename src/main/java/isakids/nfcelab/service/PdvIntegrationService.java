package isakids.nfcelab.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import isakids.nfcelab.document.NfceDocumentArtifacts;
import isakids.nfcelab.document.NfceDocumentService;
import isakids.nfcelab.dto.EmitirNfceRequest;
import isakids.nfcelab.dto.NfceItemRequest;
import isakids.nfcelab.dto.NfceOperationResponse;
import isakids.nfcelab.dto.NfceProductRequest;
import isakids.nfcelab.dto.PdvNfceIntegrationItemRequest;
import isakids.nfcelab.dto.PdvNfceIntegrationRequest;
import isakids.nfcelab.entity.EmpresaEntity;
import isakids.nfcelab.entity.FiscalModelo;
import isakids.nfcelab.entity.NotaFiscalEmpresaEntity;
import isakids.nfcelab.entity.SituacaoNotaFiscal;
import isakids.nfcelab.exception.ResourceNotFoundException;
import isakids.nfcelab.repository.EmpresaRepository;
import isakids.nfcelab.repository.NotaFiscalEmpresaRepository;

@Service
@Transactional(readOnly = true)
public class PdvIntegrationService {

    private static final String ORIGEM_SISTEMA_ISA = "ISA";

    private final EmpresaRepository empresaRepository;
    private final NotaFiscalEmpresaRepository notaFiscalEmpresaRepository;
    private final NfceLabService nfceLabService;
    private final NfceDocumentService documentService;

    public PdvIntegrationService(EmpresaRepository empresaRepository,
            NotaFiscalEmpresaRepository notaFiscalEmpresaRepository,
            NfceLabService nfceLabService,
            NfceDocumentService documentService) {
        this.empresaRepository = empresaRepository;
        this.notaFiscalEmpresaRepository = notaFiscalEmpresaRepository;
        this.nfceLabService = nfceLabService;
        this.documentService = documentService;
    }

    @Transactional
    public NfceOperationResponse emitirNfce(PdvNfceIntegrationRequest request) {
        EmpresaEntity empresa = resolveEmpresaAtivaNfce(request.getLojaCnpj());

        Optional<NotaFiscalEmpresaEntity> notaExistente = notaFiscalEmpresaRepository
                .findByEmpresaIdAndOrigemSistemaAndVendaOrigemId(empresa.getId(), ORIGEM_SISTEMA_ISA, request.getVendaId());
        if (notaExistente.filter(this::deveReutilizarNota).isPresent()) {
            return toResponse(notaExistente.get());
        }

        EmitirNfceRequest emitirRequest = toEmitirRequest(empresa, request);
        return nfceLabService.emitir(emitirRequest);
    }

    public NotaFiscalEmpresaEntity buscarNotaFiscal(String lojaCnpj, Long vendaId) {
        EmpresaEntity empresa = resolveEmpresaAtivaNfce(lojaCnpj);
        return notaFiscalEmpresaRepository
                .findByEmpresaIdAndOrigemSistemaAndVendaOrigemId(empresa.getId(), ORIGEM_SISTEMA_ISA, vendaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "NFC-e nao encontrada para a venda informada."));
    }

    @Transactional
    public byte[] loadPdf(String lojaCnpj, Long vendaId) {
        NotaFiscalEmpresaEntity nota = buscarNotaFiscal(lojaCnpj, vendaId);
        return documentService.loadPdf(nota.getChaveAcesso());
    }

    @Transactional
    public byte[] loadXml(String lojaCnpj, Long vendaId) {
        NotaFiscalEmpresaEntity nota = buscarNotaFiscal(lojaCnpj, vendaId);
        return documentService.loadXmlBytes(nota.getChaveAcesso());
    }

    private EmpresaEntity resolveEmpresaAtivaNfce(String lojaCnpj) {
        String cnpjNormalizado = normalizeDigits(lojaCnpj, "lojaCnpj");
        EmpresaEntity empresa = empresaRepository.findByCnpjAndAtivoTrue(cnpjNormalizado)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Empresa ativa nao encontrada para o CNPJ da loja informado."));
        if (!FiscalModelo.NFCE.equals(empresa.getModelo())) {
            throw new IllegalArgumentException("A empresa vinculada a loja nao esta configurada para emitir NFC-e.");
        }
        return empresa;
    }

    private EmitirNfceRequest toEmitirRequest(EmpresaEntity empresa, PdvNfceIntegrationRequest request) {
        EmitirNfceRequest emitirRequest = new EmitirNfceRequest();
        emitirRequest.setEmpresaId(empresa.getId());
        emitirRequest.setVendaId(request.getVendaId());
        emitirRequest.setOrigemSistema(ORIGEM_SISTEMA_ISA);
        emitirRequest.setNumeroNota(request.getNumeroNota());
        emitirRequest.setDataHora(request.getDataHora());
        emitirRequest.setDesconto(request.getDesconto());
        emitirRequest.setFormaPagamento(request.getFormaPagamento());
        emitirRequest.setObservacao(request.getObservacao());
        emitirRequest.setCpfCnpjConsumidor(request.getCpfCnpjConsumidor());
        emitirRequest.setItens(request.getItens().stream()
                .map(this::toItemRequest)
                .toList());
        return emitirRequest;
    }

    private NfceItemRequest toItemRequest(PdvNfceIntegrationItemRequest item) {
        NfceProductRequest product = new NfceProductRequest();
        product.setProdutoId(item.getProdutoId());
        product.setDescricao(item.getDescricao());
        product.setCodigoBarras(item.getCodigoBarras());
        product.setNcm(item.getNcm());
        product.setCest(item.getCest());
        product.setOrigem(item.getOrigem());
        product.setCfop(item.getCfop());
        product.setUnidadeComercial(item.getUnidadeComercial());

        NfceItemRequest request = new NfceItemRequest();
        request.setItemId(item.getItemId());
        request.setQuantidade(item.getQuantidade());
        request.setPrecoUnitario(item.getPrecoUnitario());
        request.setSubtotal(item.getSubtotal());
        request.setDesconto(item.getDesconto());
        request.setProduto(product);
        return request;
    }

    private NfceOperationResponse toResponse(NotaFiscalEmpresaEntity nota) {
        NfceOperationResponse response = new NfceOperationResponse();
        response.setEmpresaId(nota.getEmpresa().getId());
        response.setEmpresaNome(nota.getEmpresa().getNome());
        response.setModelo(nota.getModelo().name());
        response.setCStat(nota.getcStat());
        response.setMotivo(nota.getMotivo());
        response.setObservacao(nota.getObservacao());
        response.setChaveAcesso(nota.getChaveAcesso());
        response.setProtocoloAutorizacao(nota.getProtocoloAutorizacao());
        response.setQrCodeUrl(nota.getQrCodeUrl());

        if (StringUtils.hasText(nota.getChaveAcesso())) {
            String apiBasePath = FiscalModelo.NFCE.equals(nota.getModelo()) ? "/api/nfce" : "/api/nfe";
            response.setXmlDownloadUrl(apiBasePath + "/documentos/" + nota.getChaveAcesso() + "/xml");
            response.setPdfDownloadUrl(apiBasePath + "/documentos/" + nota.getChaveAcesso() + "/pdf");
            documentService.find(nota.getChaveAcesso()).ifPresent(artifacts -> attachPaths(response, artifacts));
        }

        return response;
    }

    private void attachPaths(NfceOperationResponse response, NfceDocumentArtifacts artifacts) {
        response.setXmlFilePath(artifacts.xmlPath().toString());
        response.setPdfFilePath(artifacts.pdfPath().toString());
    }

    private boolean deveReutilizarNota(NotaFiscalEmpresaEntity nota) {
        SituacaoNotaFiscal situacao = nota.getSituacao();
        return SituacaoNotaFiscal.AUTORIZADA.equals(situacao)
                || SituacaoNotaFiscal.PENDENTE.equals(situacao);
    }

    private String normalizeDigits(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " deve ser informado.");
        }
        return value.replaceAll("\\D", "");
    }
}
