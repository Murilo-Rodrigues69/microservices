package isakids.nfcelab.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import isakids.nfcelab.dto.PageResponse;
import isakids.nfcelab.dto.ProdutoEmpresaRequest;
import isakids.nfcelab.dto.ProdutoEmpresaResponse;
import isakids.nfcelab.entity.EmpresaEntity;
import isakids.nfcelab.entity.ProdutoEmpresaEntity;
import isakids.nfcelab.exception.ResourceNotFoundException;
import isakids.nfcelab.model.ProductData;
import isakids.nfcelab.repository.ProdutoEmpresaRepository;

@Service
@Transactional(readOnly = true)
public class ProdutoEmpresaService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final ProdutoEmpresaRepository produtoEmpresaRepository;
    private final EmpresaService empresaService;

    public ProdutoEmpresaService(ProdutoEmpresaRepository produtoEmpresaRepository, EmpresaService empresaService) {
        this.produtoEmpresaRepository = produtoEmpresaRepository;
        this.empresaService = empresaService;
    }

    public List<ProdutoEmpresaResponse> listar(Long empresaId) {
        return listar(empresaId, null);
    }

    public List<ProdutoEmpresaResponse> listar(Long empresaId, String nome) {
        empresaService.requireEmpresa(empresaId);
        String search = normalizeSearch(nome);
        List<ProdutoEmpresaEntity> produtos = search == null
                ? produtoEmpresaRepository.findByEmpresaIdOrderByDescricaoAscIdAsc(empresaId)
                : produtoEmpresaRepository.findByEmpresaIdAndDescricaoContainingIgnoreCaseOrderByDescricaoAscIdAsc(
                        empresaId, search);
        return produtos.stream()
                .map(this::toResponse)
                .toList();
    }

    public PageResponse<ProdutoEmpresaResponse> listarPaginado(Long empresaId, Integer page, Integer size, String nome) {
        empresaService.requireEmpresa(empresaId);
        Pageable pageable = PageRequest.of(
                normalizePage(page),
                normalizeSize(size),
                Sort.by(Sort.Order.asc("descricao"), Sort.Order.asc("id")));
        String search = normalizeSearch(nome);
        Page<ProdutoEmpresaEntity> produtos = search == null
                ? produtoEmpresaRepository.findByEmpresaId(empresaId, pageable)
                : produtoEmpresaRepository.findByEmpresaIdAndDescricaoContainingIgnoreCase(empresaId, search, pageable);
        return PageResponse.from(produtos.map(this::toResponse));
    }

    public ProdutoEmpresaResponse buscar(Long empresaId, Long produtoId) {
        return toResponse(requireProduto(empresaId, produtoId));
    }

    @Transactional
    public ProdutoEmpresaResponse criar(Long empresaId, ProdutoEmpresaRequest request) {
        EmpresaEntity empresa = empresaService.requireEmpresa(empresaId);
        ProdutoEmpresaEntity produto = new ProdutoEmpresaEntity();
        produto.setEmpresa(empresa);
        applyRequest(produto, request);
        return toResponse(produtoEmpresaRepository.save(produto));
    }

    @Transactional
    public ProdutoEmpresaResponse atualizar(Long empresaId, Long produtoId, ProdutoEmpresaRequest request) {
        ProdutoEmpresaEntity produto = requireProduto(empresaId, produtoId);
        applyRequest(produto, request);
        return toResponse(produtoEmpresaRepository.save(produto));
    }

    public ProductData toProductData(Long empresaId, Long produtoId) {
        ProdutoEmpresaEntity produto = produtoEmpresaRepository.findByIdAndEmpresaIdAndAtivoTrue(produtoId, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Produto ativo nao encontrado para o id " + produtoId + " na empresa " + empresaId + "."));
        return new ProductData(
                produto.getId(),
                produto.getDescricao(),
                produto.getCodigoBarras(),
                produto.getNcm(),
                produto.getCest(),
                StringUtils.hasText(produto.getOrigem()) ? produto.getOrigem() : "NACIONAL",
                produto.getCfop(),
                produto.getUnidadeComercial());
    }

    private ProdutoEmpresaEntity requireProduto(Long empresaId, Long produtoId) {
        return produtoEmpresaRepository.findByIdAndEmpresaId(produtoId, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Produto nao encontrado para o id " + produtoId + " na empresa " + empresaId + "."));
    }

    private void applyRequest(ProdutoEmpresaEntity produto, ProdutoEmpresaRequest request) {
        produto.setDescricao(requireText(request.getDescricao(), "descricao"));
        produto.setCodigoBarras(normalizeDigitsOrNull(request.getCodigoBarras()));
        produto.setNcm(normalizeDigitsOrNull(request.getNcm()));
        produto.setCest(normalizeDigitsOrNull(request.getCest()));
        produto.setOrigem(trimToNull(request.getOrigem()));
        produto.setCfop(normalizeDigitsOrNull(request.getCfop()));
        produto.setUnidadeComercial(trimToNull(request.getUnidadeComercial()));
        produto.setPrecoUnitarioSugerido(request.getPrecoUnitarioSugerido());
        produto.setAtivo(request.getAtivo() == null || request.getAtivo());
    }

    private ProdutoEmpresaResponse toResponse(ProdutoEmpresaEntity produto) {
        return new ProdutoEmpresaResponse(
                produto.getId(),
                produto.getEmpresa().getId(),
                produto.getDescricao(),
                produto.getCodigoBarras(),
                produto.getNcm(),
                produto.getCest(),
                produto.getOrigem(),
                produto.getCfop(),
                produto.getUnidadeComercial(),
                produto.getPrecoUnitarioSugerido(),
                produto.isAtivo(),
                produto.getCreatedAt(),
                produto.getUpdatedAt());
    }

    private String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " deve ser informado.");
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeDigitsOrNull(String value) {
        String trimmed = trimToNull(value);
        return trimmed != null ? trimmed.replaceAll("\\D", "") : null;
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
