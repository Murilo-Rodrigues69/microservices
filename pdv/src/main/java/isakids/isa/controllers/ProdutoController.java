package isakids.isa.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import isakids.isa.dto.EstoqueResumoDTO;
import isakids.isa.dto.ProdutoBuscaResponse;
import isakids.isa.entities.Produto;
import isakids.isa.entities.ProdutoLoja;
import isakids.isa.services.AuthenticatedUserService;
import isakids.isa.services.CodigoBarrasService;
import isakids.isa.services.ProdutoService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@RestController
@Validated
@RequestMapping("/api/produtos")
public class ProdutoController {

    @Autowired
    ProdutoService produtoService;

    @Autowired
    CodigoBarrasService codigoBarrasService;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @GetMapping("/{id}/codigo-barras")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> gerarCodigoBarras(@PathVariable Long id) {
        Produto produto = produtoService.buscarPorId(id);

        byte[] pdf = codigoBarrasService.gerarCodigoBarrasPDF(produto.getCodigoBarras());

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=codigo_barras_" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/buscar")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public List<ProdutoBuscaResponse> buscarPorNome(
            @RequestParam(value = "lojaId", required = false) Long lojaId,
            @RequestParam("nome") @NotBlank String nome,
            @RequestParam(value = "limit", required = false) @Min(1) Integer limit
    ) {
        Long resolvedLojaId = authenticatedUserService.resolveLojaId(lojaId);
        return produtoService.buscarPorNome(resolvedLojaId, nome, limit).stream()
                .map(ProdutoBuscaResponse::fromEntity)
                .toList();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<Produto> listarTodos() {
        return produtoService.buscarTodos();
    }

    @GetMapping("/resumo")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public EstoqueResumoDTO getResumoEstoque(@RequestParam(value = "lojaId", required = false) Long lojaId) {
        Long resolvedLojaId = authenticatedUserService.resolveLojaId(lojaId);
        return produtoService.obterResumoEstoque(resolvedLojaId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Produto buscarPorId(@PathVariable Long id) {
        return produtoService.buscarPorId(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Produto criar(@RequestBody Produto produto) {
        return produtoService.salvar(produto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Produto atualizar(@PathVariable Long id,
                             @RequestBody Produto produtoAtualizado) {

        Produto existente = produtoService.buscarPorId(id);

        existente.setDescricao(produtoAtualizado.getDescricao());
        existente.setCategoria(produtoAtualizado.getCategoria());
        existente.setTamanho(produtoAtualizado.getTamanho());
        existente.setCor(produtoAtualizado.getCor());
        existente.setPrecoCusto(produtoAtualizado.getPrecoCusto());
        existente.setPrecoVenda(produtoAtualizado.getPrecoVenda());
        existente.setNcm(produtoAtualizado.getNcm());
        existente.setCest(produtoAtualizado.getCest());
        existente.setOrigem(produtoAtualizado.getOrigem());
        existente.setCfop(produtoAtualizado.getCfop());
        existente.setUnidadeComercial(produtoAtualizado.getUnidadeComercial());
        existente.setAtivo(produtoAtualizado.getAtivo());

        return produtoService.salvar(existente);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deletar(@PathVariable Long id) {
        Produto produto = produtoService.buscarPorId(id);
        produto.setAtivo(false);
        produtoService.salvar(produto);
    }

    @GetMapping("/ativos")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Produto> listarAtivos() {
        return produtoService.listarAtivos();
    }

    @GetMapping("/estoque")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public List<ProdutoLoja> listarEstoqueAtual(@RequestParam(value = "lojaId", required = false) Long lojaId) {
        Long resolvedLojaId = authenticatedUserService.resolveLojaId(lojaId);
        return produtoService.listarEstoqueAtual(resolvedLojaId);
    }

    @GetMapping("/codigo-barras/{codigoBarras}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ProdutoLoja buscarPorCodigoBarras(
            @PathVariable String codigoBarras,
            @RequestParam(value = "lojaId", required = false) Long lojaId
    ) {
        Long resolvedLojaId = authenticatedUserService.resolveLojaId(lojaId);
        return produtoService.buscarPorCodigoBarrasAtivo(resolvedLojaId, codigoBarras);
    }
}
