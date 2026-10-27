package isakids.isa.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import isakids.isa.dto.EstoqueResumoDTO;
import isakids.isa.entities.Loja;
import isakids.isa.entities.Produto;
import isakids.isa.entities.ProdutoLoja;
import isakids.isa.repositories.LojaRepository;
import isakids.isa.repositories.ProdutoLojaRepository;
import isakids.isa.repositories.ProdutoRepository;
import jakarta.persistence.EntityNotFoundException;

@Service
public class ProdutoService {

    @Autowired
    ProdutoRepository produtoRepository;

    @Autowired
    ProdutoLojaRepository produtoLojaRepository;

    @Autowired
    LojaRepository lojaRepository;

    @Transactional
    public Produto salvar(Produto produto) {
        normalizarProduto(produto);

        if (!StringUtils.hasText(produto.getCodigoInterno())) {
            produto.setCodigoInterno(gerarCodigoInterno());
        }

        if (!StringUtils.hasText(produto.getCodigoBarras())) {
            produto.setCodigoBarras(gerarCodigoBarras());
        }

        return produtoRepository.save(produto);
    }

    public Produto buscarPorId(Long id) {
        return produtoRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Produto nÃ£o encontrado"));
    }

    public ProdutoLoja buscarPorCodigoBarrasAtivo(Long lojaId, String codigoBarras) {
        buscarLoja(lojaId);
        return produtoLojaRepository.findByProdutoCodigoBarrasAndLojaIdAndProdutoAtivoTrue(codigoBarras, lojaId)
                .orElseThrow(() -> new EntityNotFoundException("Produto nÃ£o encontrado para o cÃ³digo de barras: " + codigoBarras));
    }

    public List<ProdutoLoja> listarEstoqueAtual(Long lojaId) {
        buscarLoja(lojaId);
        return produtoLojaRepository.findByLojaIdAndProdutoAtivoTrue(lojaId);
    }

    public List<Produto> buscarTodos() {
        return produtoRepository.findAll();
    }

    public List<Produto> listarAtivos() {
        return produtoRepository.findByAtivoTrue();
    }

    private String gerarCodigoInterno() {
        String codigo = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        while (produtoRepository.existsByCodigoInterno(codigo)) {
            codigo = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        return codigo;
    }

    private String gerarCodigoBarras() {
        String codigo = "LJ" + UUID.randomUUID().toString().substring(0, 10).replace("-", "").toUpperCase();
        while (produtoRepository.existsByCodigoBarras(codigo)) {
            codigo = "LJ" + UUID.randomUUID().toString().substring(0, 10).replace("-", "").toUpperCase();
        }
        return codigo;
    }

    public EstoqueResumoDTO obterResumoEstoque(Long lojaId) {
        buscarLoja(lojaId);

        Long totalItens = produtoLojaRepository.sumQuantidadeEstoqueByLojaId(lojaId);
        Long estoqueBaixo = produtoLojaRepository.countProdutosEstoqueBaixoByLojaId(lojaId);
        BigDecimal valorTotal = produtoLojaRepository.sumValorTotalEstoqueByLojaId(lojaId);

        return new EstoqueResumoDTO(
                totalItens,
                estoqueBaixo,
                valorTotal
        );
    }

    public List<ProdutoLoja> buscarPorNome(Long lojaId, String termo, Integer limit) {
        buscarLoja(lojaId);
        String q = (termo == null) ? "" : termo.trim();

        if (q.isBlank()) {
            return List.of();
        }

        int l = (limit == null || limit <= 0) ? 10 : Math.min(limit, 50);

        return produtoLojaRepository.buscarPorDescricaoNaLoja(
                lojaId,
                q,
                PageRequest.of(0, l)
        );
    }

    private Loja buscarLoja(Long lojaId) {
        if (lojaId == null) {
            throw new IllegalArgumentException("lojaId e obrigatorio");
        }
        return lojaRepository.findById(lojaId)
                .orElseThrow(() -> new EntityNotFoundException("Loja nao encontrada: " + lojaId));
    }

    private void normalizarProduto(Produto produto) {
        produto.setCodigoInterno(trimToNull(produto.getCodigoInterno()));
        produto.setCodigoBarras(trimToNull(produto.getCodigoBarras()));
        produto.setDescricao(trimToNull(produto.getDescricao()));
        produto.setCategoria(trimToNull(produto.getCategoria()));
        produto.setTamanho(trimToNull(produto.getTamanho()));
        produto.setCor(trimToNull(produto.getCor()));
        produto.setNcm(normalizeDigitsOrNull(produto.getNcm(), 8, "ncm"));
        produto.setCest(normalizeDigitsOrNull(produto.getCest(), 7, "cest"));
        produto.setOrigem(normalizeUpperOrNull(produto.getOrigem(), 120, "origem"));
        produto.setCfop(normalizeDigitsOrNull(produto.getCfop(), 4, "cfop"));
        produto.setUnidadeComercial(normalizeUpperOrNull(produto.getUnidadeComercial(), 10, "unidadeComercial"));
        produto.setAtivo(produto.getAtivo() == null || produto.getAtivo());
    }

    private String normalizeDigitsOrNull(String value, int expectedSize, String fieldName) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        String normalized = trimmed.replaceAll("\\D", "");
        if (normalized.length() != expectedSize) {
            throw new IllegalArgumentException(fieldName + " deve ter " + expectedSize + " digitos.");
        }
        return normalized;
    }

    private String normalizeUpperOrNull(String value, int maxSize, String fieldName) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        String normalized = trimmed.toUpperCase();
        if (normalized.length() > maxSize) {
            throw new IllegalArgumentException(fieldName + " deve ter no maximo " + maxSize + " caracteres.");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
