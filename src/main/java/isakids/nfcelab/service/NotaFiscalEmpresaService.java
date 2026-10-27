package isakids.nfcelab.service;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import isakids.nfcelab.document.NfceDocumentService;
import isakids.nfcelab.dto.CancelarNfceRequest;
import isakids.nfcelab.dto.ConsultarNfceRequest;
import isakids.nfcelab.dto.EmitirNfceRequest;
import isakids.nfcelab.dto.NfceOperationResponse;
import isakids.nfcelab.dto.NotaFiscalEmpresaResponse;
import isakids.nfcelab.dto.PageResponse;
import isakids.nfcelab.entity.AppUserEntity;
import isakids.nfcelab.entity.ClienteEmpresaEntity;
import isakids.nfcelab.entity.EmpresaEntity;
import isakids.nfcelab.entity.FiscalModelo;
import isakids.nfcelab.entity.NotaFiscalEmpresaEntity;
import isakids.nfcelab.entity.SituacaoNotaFiscal;
import isakids.nfcelab.model.RecipientData;
import isakids.nfcelab.model.SaleData;
import isakids.nfcelab.repository.ClienteEmpresaRepository;
import isakids.nfcelab.repository.NotaFiscalEmpresaRepository;
import isakids.nfcelab.service.DocumentoFiscalXmlParser.DocumentoFiscalResumo;
import jakarta.persistence.EntityNotFoundException;

@Service
@Transactional(readOnly = true)
public class NotaFiscalEmpresaService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final NotaFiscalEmpresaRepository notaFiscalEmpresaRepository;
    private final ClienteEmpresaRepository clienteEmpresaRepository;
    private final EmpresaService empresaService;
    private final DocumentoFiscalXmlParser documentoFiscalXmlParser;
    private final NfceDocumentService documentService;
    private final AuthenticatedUserService authenticatedUserService;

    public NotaFiscalEmpresaService(NotaFiscalEmpresaRepository notaFiscalEmpresaRepository,
            ClienteEmpresaRepository clienteEmpresaRepository, EmpresaService empresaService,
            DocumentoFiscalXmlParser documentoFiscalXmlParser, NfceDocumentService documentService,
            AuthenticatedUserService authenticatedUserService) {
        this.notaFiscalEmpresaRepository = notaFiscalEmpresaRepository;
        this.clienteEmpresaRepository = clienteEmpresaRepository;
        this.empresaService = empresaService;
        this.documentoFiscalXmlParser = documentoFiscalXmlParser;
        this.documentService = documentService;
        this.authenticatedUserService = authenticatedUserService;
    }

    public PageResponse<NotaFiscalEmpresaResponse> listar(Long empresaId, Integer page, Integer size) {
        empresaService.requireEmpresa(empresaId);
        Pageable pageable = PageRequest.of(normalizePage(page), normalizeSize(size));
        return PageResponse.from(notaFiscalEmpresaRepository.findPageByEmpresaId(empresaId, pageable)
                .map(this::toResponse));
    }

    @Transactional
    public void registrarEmissao(EmpresaEntity empresa, EmitirNfceRequest request, SaleData saleData,
            RecipientData destinatario, NfceOperationResponse response) {
        String chaveAcesso = resolveChaveAcesso(response.getChaveAcesso(), null);
        if (chaveAcesso == null) {
            return;
        }

        DocumentoFiscalResumo resumo = parseResumo(response.getXml());
        NotaFiscalEmpresaEntity nota = resolveExistingNota(empresa, chaveAcesso, request.getOrigemSistema(),
                request.getVendaId());
        nota.setCliente(resolveCliente(empresa.getId(), request.getClienteEmpresaId()).orElse(null));
        applyUsuarioEmissor(nota);
        applyCommonState(nota, empresa, response);
        applyResumo(nota, resumo);
        nota.setNumeroNota(firstNonBlank(nota.getNumeroNota(), saleData.numeroNota()));
        nota.setSerie(firstNonBlank(nota.getSerie(), empresa.getSerie()));
        nota.setDestinatarioNome(firstNonBlank(nota.getDestinatarioNome(), destinatario != null ? destinatario.nome() : null));
        nota.setDestinatarioDocumento(firstNonBlank(nota.getDestinatarioDocumento(),
                destinatario != null ? destinatario.documento() : null));
        nota.setObservacao(firstNonBlank(nota.getObservacao(), saleData.observacao()));
        nota.setValorTotal(firstNonNull(nota.getValorTotal(), calcularValorTotal(saleData)));
        nota.setDataEmissao(firstNonNull(nota.getDataEmissao(), saleData.dataHora()));
        nota.setOrigemSistema(trimToNull(request.getOrigemSistema()));
        nota.setVendaOrigemId(request.getVendaId());
        nota.setSituacao(resolveSituacao(response.getCStat(), SituacaoNotaFiscal.AUTORIZADA));
        notaFiscalEmpresaRepository.save(nota);
    }

    @Transactional
    public void registrarConsulta(EmpresaEntity empresa, ConsultarNfceRequest request, NfceOperationResponse response) {
        String chaveAcesso = resolveChaveAcesso(response.getChaveAcesso(), request.getChaveAcesso());
        if (chaveAcesso == null) {
            return;
        }

        DocumentoFiscalResumo resumo = parseResumo(response.getXml());
        Optional<NotaFiscalEmpresaEntity> existente = notaFiscalEmpresaRepository.findByChaveAcesso(chaveAcesso);
        if (existente.isEmpty() && !devePersistirSemHistorico(resumo, response.getCStat(), empresa)) {
            return;
        }

        NotaFiscalEmpresaEntity nota = existente.orElseGet(() -> createNota(empresa, chaveAcesso));
        applyCommonState(nota, empresa, response);
        applyResumo(nota, resumo);
        nota.setSituacao(resolveSituacao(response.getCStat(), nota.getSituacao()));
        notaFiscalEmpresaRepository.save(nota);
    }

    @Transactional
    public void registrarCancelamento(EmpresaEntity empresa, CancelarNfceRequest request, NfceOperationResponse response) {
        String chaveAcesso = resolveChaveAcesso(response.getChaveAcesso(), request.getChaveAcesso());
        if (chaveAcesso == null) {
            return;
        }

        DocumentoFiscalResumo resumo = parseResumo(documentService.loadXml(chaveAcesso));
        Optional<NotaFiscalEmpresaEntity> existente = notaFiscalEmpresaRepository.findByChaveAcesso(chaveAcesso);
        if (existente.isEmpty() && !devePersistirSemHistorico(resumo, response.getCStat(), empresa)) {
            return;
        }

        NotaFiscalEmpresaEntity nota = existente.orElseGet(() -> createNota(empresa, chaveAcesso));
        String protocoloExistente = nota.getProtocoloAutorizacao();
        applyCommonState(nota, empresa, response);
        applyResumo(nota, resumo);
        nota.setProtocoloAutorizacao(firstNonBlank(protocoloExistente,
                firstNonBlank(nota.getProtocoloAutorizacao(), resumo != null ? resumo.protocoloAutorizacao() : null)));
        nota.setSituacao(resolveSituacao(response.getCStat(), SituacaoNotaFiscal.CANCELADA));
        notaFiscalEmpresaRepository.save(nota);
    }

    private NotaFiscalEmpresaEntity requireOrCreate(EmpresaEntity empresa, String chaveAcesso) {
        return notaFiscalEmpresaRepository.findByChaveAcesso(chaveAcesso)
                .map(existing -> {
                    existing.setEmpresa(empresa);
                    return existing;
                })
                .orElseGet(() -> createNota(empresa, chaveAcesso));
    }

    private NotaFiscalEmpresaEntity resolveExistingNota(EmpresaEntity empresa, String chaveAcesso, String origemSistema,
            Long vendaOrigemId) {
        String origemNormalizada = trimToNull(origemSistema);
        if (origemNormalizada != null && vendaOrigemId != null) {
            Optional<NotaFiscalEmpresaEntity> existentePorOrigem = notaFiscalEmpresaRepository
                    .findByEmpresaIdAndOrigemSistemaAndVendaOrigemId(empresa.getId(), origemNormalizada, vendaOrigemId);
            if (existentePorOrigem.isPresent()) {
                NotaFiscalEmpresaEntity nota = existentePorOrigem.get();
                nota.setEmpresa(empresa);
                return nota;
            }
        }
        return requireOrCreate(empresa, chaveAcesso);
    }

    private void applyCommonState(NotaFiscalEmpresaEntity nota, EmpresaEntity empresa, NfceOperationResponse response) {
        nota.setEmpresa(empresa);
        nota.setModelo(empresa.getModelo());
        nota.setChaveAcesso(resolveChaveAcesso(response.getChaveAcesso(), nota.getChaveAcesso()));
        nota.setProtocoloAutorizacao(firstNonBlank(trimToNull(response.getProtocoloAutorizacao()),
                nota.getProtocoloAutorizacao()));
        nota.setcStat(response.getCStat());
        nota.setMotivo(trimToNull(response.getMotivo()));
        nota.setQrCodeUrl(firstNonBlank(trimToNull(response.getQrCodeUrl()), nota.getQrCodeUrl()));
    }

    private void applyResumo(NotaFiscalEmpresaEntity nota, DocumentoFiscalResumo resumo) {
        if (resumo == null) {
            return;
        }

        nota.setNumeroNota(firstNonBlank(trimToNull(resumo.numeroNota()), nota.getNumeroNota()));
        nota.setSerie(firstNonBlank(trimToNull(resumo.serie()), nota.getSerie()));
        nota.setDataEmissao(firstNonNull(nota.getDataEmissao(), resumo.dataEmissao()));
        nota.setDestinatarioNome(firstNonBlank(trimToNull(resumo.destinatarioNome()), nota.getDestinatarioNome()));
        nota.setDestinatarioDocumento(firstNonBlank(trimToNull(resumo.destinatarioDocumento()),
                nota.getDestinatarioDocumento()));
        nota.setObservacao(firstNonBlank(trimToNull(resumo.observacao()), nota.getObservacao()));
        nota.setValorTotal(firstNonNull(nota.getValorTotal(), resumo.valorTotal()));
        nota.setProtocoloAutorizacao(firstNonBlank(trimToNull(resumo.protocoloAutorizacao()),
                nota.getProtocoloAutorizacao()));
    }

    private DocumentoFiscalResumo parseResumo(String xml) {
        return documentoFiscalXmlParser.parse(xml).orElse(null);
    }

    private NotaFiscalEmpresaEntity createNota(EmpresaEntity empresa, String chaveAcesso) {
        NotaFiscalEmpresaEntity nota = new NotaFiscalEmpresaEntity();
        nota.setEmpresa(empresa);
        nota.setChaveAcesso(chaveAcesso);
        nota.setModelo(empresa.getModelo());
        nota.setSituacao(SituacaoNotaFiscal.DESCONHECIDA);
        return nota;
    }

    private void applyUsuarioEmissor(NotaFiscalEmpresaEntity nota) {
        resolveCurrentUser().ifPresent(usuario -> {
            nota.setUsuarioEmissor(usuario);
            nota.setUsuarioEmissorLogin(usuario.getLogin());
            nota.setUsuarioEmissorNome(usuario.getName());
        });
    }

    private Optional<AppUserEntity> resolveCurrentUser() {
        try {
            return Optional.of(authenticatedUserService.getCurrentUser());
        } catch (AccessDeniedException | EntityNotFoundException e) {
            return Optional.empty();
        }
    }

    private Optional<ClienteEmpresaEntity> resolveCliente(Long empresaId, Long clienteEmpresaId) {
        if (clienteEmpresaId == null) {
            return Optional.empty();
        }
        return clienteEmpresaRepository.findByIdAndEmpresaId(clienteEmpresaId, empresaId);
    }

    private SituacaoNotaFiscal resolveSituacao(Integer cStat, SituacaoNotaFiscal fallback) {
        if (cStat == null) {
            return fallback != null ? fallback : SituacaoNotaFiscal.DESCONHECIDA;
        }

        return switch (cStat) {
            case 100, 150 -> SituacaoNotaFiscal.AUTORIZADA;
            case 101, 135, 151, 155 -> SituacaoNotaFiscal.CANCELADA;
            default -> {
                if (cStat >= 500) {
                    yield SituacaoNotaFiscal.ERRO;
                }
                if (cStat >= 200) {
                    yield SituacaoNotaFiscal.NAO_AUTORIZADA;
                }
                yield fallback != null ? fallback : SituacaoNotaFiscal.PENDENTE;
            }
        };
    }

    private boolean devePersistirSemHistorico(DocumentoFiscalResumo resumo, Integer cStat, EmpresaEntity empresa) {
        if (resumo != null) {
            return resumo.emitenteCnpj() == null || resumo.emitenteCnpj().equals(empresa.getCnpj());
        }

        SituacaoNotaFiscal situacao = resolveSituacao(cStat, SituacaoNotaFiscal.DESCONHECIDA);
        return SituacaoNotaFiscal.AUTORIZADA.equals(situacao) || SituacaoNotaFiscal.CANCELADA.equals(situacao);
    }

    private BigDecimal calcularValorTotal(SaleData saleData) {
        return saleData.valorTotalNota();
    }

    private NotaFiscalEmpresaResponse toResponse(NotaFiscalEmpresaEntity nota) {
        String apiBasePath = FiscalModelo.NFCE.equals(nota.getModelo()) ? "/api/nfce" : "/api/nfe";
        return new NotaFiscalEmpresaResponse(
                nota.getId(),
                nota.getEmpresa().getId(),
                nota.getCliente() != null ? nota.getCliente().getId() : null,
                nota.getUsuarioEmissor() != null ? nota.getUsuarioEmissor().getId() : null,
                firstNonBlank(nota.getUsuarioEmissorLogin(),
                        nota.getUsuarioEmissor() != null ? nota.getUsuarioEmissor().getLogin() : null),
                firstNonBlank(nota.getUsuarioEmissorNome(),
                        nota.getUsuarioEmissor() != null ? nota.getUsuarioEmissor().getName() : null),
                nota.getModelo(),
                nota.getSituacao(),
                nota.getNumeroNota(),
                nota.getSerie(),
                nota.getChaveAcesso(),
                nota.getProtocoloAutorizacao(),
                nota.getcStat(),
                nota.getMotivo(),
                nota.getObservacao(),
                nota.getQrCodeUrl(),
                nota.getDestinatarioNome(),
                nota.getDestinatarioDocumento(),
                nota.getValorTotal(),
                nota.getDataEmissao(),
                nota.getChaveAcesso() != null ? apiBasePath + "/documentos/" + nota.getChaveAcesso() + "/xml" : null,
                nota.getChaveAcesso() != null ? apiBasePath + "/documentos/" + nota.getChaveAcesso() + "/pdf" : null,
                nota.getCreatedAt(),
                nota.getUpdatedAt());
    }

    private String resolveChaveAcesso(String primary, String fallback) {
        String normalizedPrimary = normalizeDigits(primary);
        if (normalizedPrimary != null) {
            return normalizedPrimary;
        }
        return normalizeDigits(fallback);
    }

    private String normalizeDigits(String value) {
        String trimmed = trimToNull(value);
        return trimmed != null ? trimmed.replaceAll("\\D", "") : null;
    }

    private String firstNonBlank(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private int normalizePage(Integer page) {
        return page != null && page >= 0 ? page : DEFAULT_PAGE;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
