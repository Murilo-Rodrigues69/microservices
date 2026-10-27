package isakids.nfcelab.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fincatto.documentofiscal.DFModelo;
import com.fincatto.documentofiscal.DFUnidadeFederativa;
import com.fincatto.documentofiscal.nfe400.classes.statusservico.consulta.NFStatusServicoConsultaRetorno;
import com.fincatto.documentofiscal.nfe400.webservices.WSFacade;

import isakids.nfcelab.dto.CancelarNfceRequest;
import isakids.nfcelab.dto.ConsultarNfceRequest;
import isakids.nfcelab.dto.EmitirNfceRequest;
import isakids.nfcelab.dto.NfceItemRequest;
import isakids.nfcelab.dto.NfceOperationResponse;
import isakids.nfcelab.dto.NfceProductRequest;
import isakids.nfcelab.dto.NfceStatusResponse;
import isakids.nfcelab.document.NfceDocumentArtifacts;
import isakids.nfcelab.document.NfceDocumentService;
import isakids.nfcelab.entity.EmpresaEntity;
import isakids.nfcelab.fiscal.NfceEmissionResult;
import isakids.nfcelab.fiscal.NfceEmitter;
import isakids.nfcelab.fiscal.FiscalContext;
import isakids.nfcelab.fiscal.FiscalContextFactory;
import isakids.nfcelab.fiscal.NfceXmlFactory;
import isakids.nfcelab.model.ProductData;
import isakids.nfcelab.model.RecipientData;
import isakids.nfcelab.model.SaleData;
import isakids.nfcelab.model.SaleItemData;

@Service
public class NfceLabService {

    private final NfceEmitter emitter;
    private final FiscalContextFactory fiscalContextFactory;
    private final EmpresaService empresaService;
    private final ProdutoEmpresaService produtoEmpresaService;
    private final ClienteEmpresaService clienteEmpresaService;
    private final NfceDocumentService documentService;
    private final NotaFiscalEmpresaService notaFiscalEmpresaService;
    private final DocumentoFiscalXmlParser documentoFiscalXmlParser;

    public NfceLabService(NfceEmitter emitter, FiscalContextFactory fiscalContextFactory, EmpresaService empresaService,
            ProdutoEmpresaService produtoEmpresaService, ClienteEmpresaService clienteEmpresaService,
            NfceDocumentService documentService, NotaFiscalEmpresaService notaFiscalEmpresaService,
            DocumentoFiscalXmlParser documentoFiscalXmlParser) {
        this.emitter = emitter;
        this.fiscalContextFactory = fiscalContextFactory;
        this.empresaService = empresaService;
        this.produtoEmpresaService = produtoEmpresaService;
        this.clienteEmpresaService = clienteEmpresaService;
        this.documentService = documentService;
        this.notaFiscalEmpresaService = notaFiscalEmpresaService;
        this.documentoFiscalXmlParser = documentoFiscalXmlParser;
    }

    public NfceStatusResponse consultarStatus(Long empresaId) throws Exception {
        EmpresaEntity empresa = empresaService.resolveEmpresaAtiva(empresaId);
        FiscalContext fiscalContext = fiscalContextFactory.create(empresa);
        WSFacade wsFacade = requireWsFacade(fiscalContext.wsFacade());

        DFUnidadeFederativa uf = fiscalContext.nfeConfig().getCUF();
        NFStatusServicoConsultaRetorno retorno = wsFacade.consultaStatus(uf, fiscalContext.nfeConfig().getModelo());

        NfceStatusResponse response = new NfceStatusResponse();
        response.setEmpresaId(empresa.getId());
        response.setEmpresaNome(empresa.getNome());
        response.setModelo(fiscalContext.nfeConfig().getModelo().name());
        response.setAmbiente(retorno.getAmbiente() != null ? retorno.getAmbiente().name() : null);
        response.setUf(retorno.getUf() != null ? retorno.getUf().name() : uf.name());
        response.setCStat(parseIntSafe(retorno.getStatus()));
        response.setMotivo(retorno.getMotivo());
        response.setVersaoAplicacao(retorno.getVersaoAplicacao());
        return response;
    }

    public NfceOperationResponse gerarXmlPreview(EmitirNfceRequest request) {
        EmpresaEntity empresa = empresaService.resolveEmpresaAtiva(request.getEmpresaId());
        try {
            FiscalContext fiscalContext = fiscalContextFactory.create(empresa);
            SaleData saleData = toSaleData(empresa.getId(), request);
            RecipientData destinatario = toRecipientData(empresa.getId(), request);
            String xml = new NfceXmlFactory(fiscalContext.nfeConfig(), fiscalContext.issuerProperties())
                    .gerarNotaFiscal(saleData, destinatario)
                    .toString();

            NfceEmissionResult result = NfceEmissionResult.builder()
                    .motivo("XML gerado sem envio para a SEFAZ.")
                    .xmlAutorizado(xml)
                    .build();
            NfceOperationResponse response = NfceOperationResponse.fromResult(result);
            applyObservacao(response, saleData.observacao());
            applyEmpresaContext(response, empresa);
            return response;
        } catch (Exception e) {
            throw new IllegalStateException("Falha gerando XML para a empresa " + empresa.getNome() + ": " + e.getMessage(), e);
        }
    }

    public NfceOperationResponse emitir(EmitirNfceRequest request) {
        EmpresaEntity empresa = empresaService.resolveEmpresaAtiva(request.getEmpresaId());
        SaleData saleData = toSaleData(empresa.getId(), request);
        RecipientData destinatario = toRecipientData(empresa.getId(), request);
        NfceOperationResponse response = NfceOperationResponse.fromResult(emitter.emitir(empresa, saleData, destinatario));
        applyEmpresaContext(response, empresa);
        response = enrichWithDocuments(response, empresa);
        applyObservacao(response, saleData.observacao());
        notaFiscalEmpresaService.registrarEmissao(empresa, request, saleData, destinatario, response);
        return response;
    }

    public NfceOperationResponse consultar(ConsultarNfceRequest request) {
        EmpresaEntity empresa = empresaService.resolveEmpresaAtiva(request.getEmpresaId());
        NfceOperationResponse response = NfceOperationResponse.fromResult(emitter.consultarPorChave(empresa, request.getChaveAcesso()));
        applyEmpresaContext(response, empresa);
        response = enrichWithDocuments(response, empresa);
        applyObservacao(response, null);
        notaFiscalEmpresaService.registrarConsulta(empresa, request, response);
        return response;
    }

    public NfceOperationResponse cancelar(CancelarNfceRequest request) {
        EmpresaEntity empresa = empresaService.resolveEmpresaAtiva(request.getEmpresaId());
        NfceOperationResponse response = NfceOperationResponse.fromResult(
                emitter.cancelar(empresa, request.getChaveAcesso(), request.getProtocoloAutorizacao(), request.getJustificativa()));
        applyEmpresaContext(response, empresa);
        notaFiscalEmpresaService.registrarCancelamento(empresa, request, response);
        return response;
    }

    private SaleData toSaleData(Long empresaId, EmitirNfceRequest request) {
        List<SaleItemData> items = request.getItens().stream()
                .map(item -> toSaleItemData(empresaId, item))
                .toList();

        return new SaleData(
                request.getVendaId(),
                request.getNumeroNota(),
                request.getDataHora() != null ? request.getDataHora() : LocalDateTime.now(),
                request.getDesconto(),
                request.getFormaPagamento(),
                trimToNull(request.getObservacao()),
                items);
    }

    private SaleItemData toSaleItemData(Long empresaId, NfceItemRequest itemRequest) {
        return new SaleItemData(
                itemRequest.getItemId(),
                itemRequest.getQuantidade(),
                itemRequest.getPrecoUnitario(),
                itemRequest.getSubtotal(),
                itemRequest.getDesconto(),
                toProductData(empresaId, itemRequest));
    }

    private ProductData toProductData(Long empresaId, NfceItemRequest itemRequest) {
        if (itemRequest.getProdutoEmpresaId() != null) {
            return produtoEmpresaService.toProductData(empresaId, itemRequest.getProdutoEmpresaId());
        }
        if (itemRequest.getProduto() == null) {
            throw new IllegalArgumentException("Cada item deve informar produtoEmpresaId ou os dados completos do produto.");
        }
        return toProductData(itemRequest.getProduto());
    }

    private ProductData toProductData(NfceProductRequest productRequest) {
        return new ProductData(
                productRequest.getProdutoId(),
                productRequest.getDescricao(),
                productRequest.getCodigoBarras(),
                productRequest.getNcm(),
                productRequest.getCest(),
                productRequest.getOrigem(),
                productRequest.getCfop(),
                productRequest.getUnidadeComercial());
    }

    private RecipientData toRecipientData(Long empresaId, EmitirNfceRequest request) {
        if (request.getClienteEmpresaId() != null) {
            return clienteEmpresaService.toRecipientData(empresaId, request.getClienteEmpresaId());
        }
        if (request.getDestinatario() != null) {
            return new RecipientData(
                    request.getDestinatario().getNome() != null ? request.getDestinatario().getNome().trim() : null,
                    normalizeDocument(request.getDestinatario().getCpfCnpj(), "destinatario.cpfCnpj"),
                    normalizeInscricaoEstadual(request.getDestinatario().getInscricaoEstadual()),
                    normalizeCep(request.getDestinatario().getCep()),
                    trimToNull(request.getDestinatario().getLogradouro()),
                    trimToNull(request.getDestinatario().getNumero()),
                    trimToNull(request.getDestinatario().getBairro()),
                    onlyDigitsOrNull(request.getDestinatario().getCodigoMunicipio()),
                    trimToNull(request.getDestinatario().getDescricaoMunicipio()),
                    normalizeUf(request.getDestinatario().getUf()),
                    trimToNull(request.getDestinatario().getEmail()));
        }

        if (StringUtils.hasText(request.getCpfCnpjConsumidor())) {
            return new RecipientData(
                    "CONSUMIDOR FINAL",
                    normalizeDocument(request.getCpfCnpjConsumidor(), "cpfCnpjConsumidor"),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);
        }

        return null;
    }

    private String normalizeDocument(String document, String fieldName) {
        if (!StringUtils.hasText(document)) {
            return null;
        }
        String normalized = document.replaceAll("\\D", "");
        if (normalized.length() != 11 && normalized.length() != 14) {
            throw new IllegalArgumentException(fieldName + " deve ter 11 ou 14 digitos.");
        }
        return normalized;
    }

    private String normalizeInscricaoEstadual(String inscricaoEstadual) {
        String trimmed = trimToNull(inscricaoEstadual);
        if (trimmed == null) {
            return null;
        }
        if ("ISENTO".equalsIgnoreCase(trimmed)) {
            return "ISENTO";
        }
        return trimmed.replaceAll("\\D", "");
    }

    private String normalizeCep(String cep) {
        if (!StringUtils.hasText(cep)) {
            return null;
        }
        String normalized = cep.replaceAll("\\D", "");
        if (normalized.length() != 8) {
            throw new IllegalArgumentException("destinatario.cep deve ter 8 digitos.");
        }
        return normalized;
    }

    private String normalizeUf(String uf) {
        String trimmed = trimToNull(uf);
        return trimmed != null ? trimmed.toUpperCase() : null;
    }

    private String onlyDigitsOrNull(String value) {
        String trimmed = trimToNull(value);
        return trimmed != null ? trimmed.replaceAll("\\D", "") : null;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private Integer parseIntSafe(String value) {
        try {
            return value == null ? null : Integer.valueOf(value);
        } catch (Exception e) {
            return null;
        }
    }

    public byte[] loadPdf(String chaveAcesso) {
        return documentService.loadPdf(chaveAcesso);
    }

    public byte[] loadXml(String chaveAcesso) {
        return documentService.loadXmlBytes(chaveAcesso);
    }

    private NfceOperationResponse enrichWithDocuments(NfceOperationResponse response, EmpresaEntity empresa) {
        if (!StringUtils.hasText(response.getChaveAcesso())) {
            return response;
        }

        if (StringUtils.hasText(response.getXml())) {
            NfceDocumentArtifacts artifacts = documentService.store(response.getChaveAcesso(), response.getXml());
            attachDocumentMetadata(response, artifacts, empresa.getModelo());
            return response;
        }

        documentService.find(response.getChaveAcesso()).ifPresent(artifacts -> {
            attachDocumentMetadata(response, artifacts, empresa.getModelo());
            if (!StringUtils.hasText(response.getXml())) {
                response.setXml(documentService.loadXml(response.getChaveAcesso()));
            }
        });

        return response;
    }

    private void attachDocumentMetadata(NfceOperationResponse response, NfceDocumentArtifacts artifacts,
            isakids.nfcelab.entity.FiscalModelo modelo) {
        String apiBasePath = isakids.nfcelab.entity.FiscalModelo.NFCE.equals(modelo) ? "/api/nfce" : "/api/nfe";
        response.setXmlDownloadUrl(apiBasePath + "/documentos/" + response.getChaveAcesso() + "/xml");
        response.setPdfDownloadUrl(apiBasePath + "/documentos/" + response.getChaveAcesso() + "/pdf");
        response.setXmlFilePath(artifacts.xmlPath().toString());
        response.setPdfFilePath(artifacts.pdfPath().toString());
    }

    private void applyEmpresaContext(NfceOperationResponse response, EmpresaEntity empresa) {
        response.setEmpresaId(empresa.getId());
        response.setEmpresaNome(empresa.getNome());
        response.setModelo(empresa.getModelo().name());
    }

    private void applyObservacao(NfceOperationResponse response, String fallbackObservacao) {
        String observacaoXml = documentoFiscalXmlParser.parse(response.getXml())
                .map(DocumentoFiscalXmlParser.DocumentoFiscalResumo::observacao)
                .orElse(null);
        response.setObservacao(StringUtils.hasText(observacaoXml) ? observacaoXml : fallbackObservacao);
    }

    private WSFacade requireWsFacade(WSFacade wsFacade) {
        if (wsFacade == null) {
            throw new IllegalStateException("Webservice da SEFAZ desabilitado. Ajuste app.nfce.ws-enabled=true.");
        }
        return wsFacade;
    }
}
