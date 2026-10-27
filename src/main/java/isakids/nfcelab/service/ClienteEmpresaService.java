package isakids.nfcelab.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import isakids.nfcelab.dto.ClienteEmpresaRequest;
import isakids.nfcelab.dto.ClienteEmpresaResponse;
import isakids.nfcelab.dto.PageResponse;
import isakids.nfcelab.entity.ClienteEmpresaEntity;
import isakids.nfcelab.entity.EmpresaEntity;
import isakids.nfcelab.exception.ResourceNotFoundException;
import isakids.nfcelab.model.RecipientData;
import isakids.nfcelab.repository.ClienteEmpresaRepository;

@Service
@Transactional(readOnly = true)
public class ClienteEmpresaService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final ClienteEmpresaRepository clienteEmpresaRepository;
    private final EmpresaService empresaService;

    public ClienteEmpresaService(ClienteEmpresaRepository clienteEmpresaRepository, EmpresaService empresaService) {
        this.clienteEmpresaRepository = clienteEmpresaRepository;
        this.empresaService = empresaService;
    }

    public List<ClienteEmpresaResponse> listar(Long empresaId) {
        return listar(empresaId, null);
    }

    public List<ClienteEmpresaResponse> listar(Long empresaId, String nome) {
        empresaService.requireEmpresa(empresaId);
        String search = normalizeSearch(nome);
        List<ClienteEmpresaEntity> clientes = search == null
                ? clienteEmpresaRepository.findByEmpresaIdOrderByNomeAscIdAsc(empresaId)
                : clienteEmpresaRepository.findByEmpresaIdAndNomeContainingIgnoreCaseOrderByNomeAscIdAsc(
                        empresaId, search);
        return clientes.stream()
                .map(this::toResponse)
                .toList();
    }

    public PageResponse<ClienteEmpresaResponse> listarPaginado(Long empresaId, Integer page, Integer size) {
        return listarPaginado(empresaId, page, size, null);
    }

    public PageResponse<ClienteEmpresaResponse> listarPaginado(Long empresaId, Integer page, Integer size, String nome) {
        empresaService.requireEmpresa(empresaId);
        Pageable pageable = PageRequest.of(
                normalizePage(page),
                normalizeSize(size),
                Sort.by(Sort.Order.asc("nome"), Sort.Order.asc("id")));
        String search = normalizeSearch(nome);
        Page<ClienteEmpresaEntity> clientes = search == null
                ? clienteEmpresaRepository.findByEmpresaId(empresaId, pageable)
                : clienteEmpresaRepository.findByEmpresaIdAndNomeContainingIgnoreCase(empresaId, search, pageable);
        return PageResponse.from(clientes.map(this::toResponse));
    }

    public ClienteEmpresaResponse buscar(Long empresaId, Long clienteId) {
        return toResponse(requireCliente(empresaId, clienteId));
    }

    @Transactional
    public ClienteEmpresaResponse criar(Long empresaId, ClienteEmpresaRequest request) {
        EmpresaEntity empresa = empresaService.requireEmpresa(empresaId);
        ClienteEmpresaEntity cliente = new ClienteEmpresaEntity();
        cliente.setEmpresa(empresa);
        applyRequest(cliente, request);
        validarDocumentoUnico(cliente);
        return toResponse(clienteEmpresaRepository.save(cliente));
    }

    @Transactional
    public ClienteEmpresaResponse atualizar(Long empresaId, Long clienteId, ClienteEmpresaRequest request) {
        ClienteEmpresaEntity cliente = requireCliente(empresaId, clienteId);
        applyRequest(cliente, request);
        validarDocumentoUnico(cliente);
        return toResponse(clienteEmpresaRepository.save(cliente));
    }

    public RecipientData toRecipientData(Long empresaId, Long clienteId) {
        ClienteEmpresaEntity cliente = clienteEmpresaRepository.findByIdAndEmpresaIdAndAtivoTrue(clienteId, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cliente ativo nao encontrado para o id " + clienteId + " na empresa " + empresaId + "."));
        return new RecipientData(
                cliente.getNome(),
                cliente.getCpfCnpj(),
                cliente.getInscricaoEstadual(),
                cliente.getCep(),
                cliente.getLogradouro(),
                cliente.getNumero(),
                cliente.getBairro(),
                cliente.getCodigoMunicipio(),
                cliente.getDescricaoMunicipio(),
                cliente.getUf(),
                cliente.getEmail());
    }

    private ClienteEmpresaEntity requireCliente(Long empresaId, Long clienteId) {
        return clienteEmpresaRepository.findByIdAndEmpresaId(clienteId, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cliente nao encontrado para o id " + clienteId + " na empresa " + empresaId + "."));
    }

    private void validarDocumentoUnico(ClienteEmpresaEntity cliente) {
        clienteEmpresaRepository.findByEmpresaIdAndCpfCnpj(cliente.getEmpresa().getId(), cliente.getCpfCnpj())
                .filter(existing -> !existing.getId().equals(cliente.getId()))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Ja existe um cliente cadastrado com esse CPF/CNPJ na empresa.");
                });
    }

    private void applyRequest(ClienteEmpresaEntity cliente, ClienteEmpresaRequest request) {
        cliente.setNome(requireText(request.getNome(), "nome"));
        cliente.setCpfCnpj(normalizeDigits(request.getCpfCnpj(), null, "cpfCnpj"));
        cliente.setInscricaoEstadual(normalizeInscricaoEstadualOrNull(request.getInscricaoEstadual()));
        cliente.setCep(normalizeDigits(request.getCep(), 8, "cep"));
        cliente.setLogradouro(trimToNull(request.getLogradouro()));
        cliente.setNumero(trimToNull(request.getNumero()));
        cliente.setBairro(trimToNull(request.getBairro()));
        cliente.setCodigoMunicipio(normalizeDigitsOrNull(request.getCodigoMunicipio()));
        cliente.setDescricaoMunicipio(trimToNull(request.getDescricaoMunicipio()));
        cliente.setUf(normalizeUfOrNull(request.getUf()));
        cliente.setEmail(trimToNull(request.getEmail()));
        cliente.setAtivo(request.getAtivo() == null || request.getAtivo());
    }

    private ClienteEmpresaResponse toResponse(ClienteEmpresaEntity cliente) {
        return new ClienteEmpresaResponse(
                cliente.getId(),
                cliente.getEmpresa().getId(),
                cliente.getNome(),
                cliente.getCpfCnpj(),
                cliente.getInscricaoEstadual(),
                cliente.getCep(),
                cliente.getLogradouro(),
                cliente.getNumero(),
                cliente.getBairro(),
                cliente.getCodigoMunicipio(),
                cliente.getDescricaoMunicipio(),
                cliente.getUf(),
                cliente.getEmail(),
                cliente.isAtivo(),
                cliente.getCreatedAt(),
                cliente.getUpdatedAt());
    }

    private String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " deve ser informado.");
        }
        return value.trim();
    }

    private String normalizeDigits(String value, Integer expectedSize, String fieldName) {
        String normalized = requireText(value, fieldName).replaceAll("\\D", "");
        if (expectedSize != null && normalized.length() != expectedSize) {
            throw new IllegalArgumentException(fieldName + " deve ter " + expectedSize + " digitos.");
        }
        if (expectedSize == null && normalized.length() != 11 && normalized.length() != 14) {
            throw new IllegalArgumentException(fieldName + " deve ter 11 ou 14 digitos.");
        }
        return normalized;
    }

    private String normalizeDigitsOrNull(String value) {
        String trimmed = trimToNull(value);
        return trimmed != null ? trimmed.replaceAll("\\D", "") : null;
    }

    private String normalizeInscricaoEstadualOrNull(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        if ("ISENTO".equalsIgnoreCase(trimmed)) {
            return "ISENTO";
        }
        return trimmed.replaceAll("\\D", "");
    }

    private String normalizeUfOrNull(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        String normalized = trimmed.toUpperCase();
        if (normalized.length() != 2) {
            throw new IllegalArgumentException("uf deve ter 2 caracteres.");
        }
        return normalized;
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

    private String normalizeSearch(String nome) {
        return trimToNull(nome);
    }
}
