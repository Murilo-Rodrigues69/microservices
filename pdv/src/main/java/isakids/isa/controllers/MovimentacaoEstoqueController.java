package isakids.isa.controllers;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import isakids.isa.dto.EntradaEstoqueRequest;
import isakids.isa.dto.SaidaEstoqueRequest;
import isakids.isa.entities.MovimentacaoEstoque;
import isakids.isa.services.AuthenticatedUserService;
import isakids.isa.services.MovimentacaoEstoqueService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/estoque/movimentacoes")
public class MovimentacaoEstoqueController {

    @Autowired
    MovimentacaoEstoqueService estoqueService;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public List<MovimentacaoEstoque> listarTodas(@RequestParam(required = false) Long lojaId) {
        Long resolvedLojaId = authenticatedUserService.resolveLojaId(lojaId);
        return estoqueService.listarTodas(resolvedLojaId);
    }

    @GetMapping("/periodo")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public List<MovimentacaoEstoque> listarPorPeriodo(
            @RequestParam(required = false) Long lojaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim
    ) {
        Long resolvedLojaId = authenticatedUserService.resolveLojaId(lojaId);
        return estoqueService.listarPorPeriodo(inicio, fim, resolvedLojaId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public MovimentacaoEstoque buscarPorId(@PathVariable Long id) {
        MovimentacaoEstoque movimentacao = estoqueService.buscarPorId(id);
        if (!authenticatedUserService.isCurrentUserAdmin()) {
            authenticatedUserService.resolveLojaId(movimentacao.getLoja().getId());
        }
        return movimentacao;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public MovimentacaoEstoque criar(@RequestBody MovimentacaoEstoque mov) {
        return estoqueService.salvar(mov);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deletar(@PathVariable Long id) {
       estoqueService.deletar(id);
    }

    @PostMapping("/entrada")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public MovimentacaoEstoque registrarEntrada(@Valid @RequestBody EntradaEstoqueRequest request) {
        Long resolvedLojaId = authenticatedUserService.resolveLojaId(request.getLojaId());
        return estoqueService.registrarEntradaMercadoria(
                request.getProdutoId(),
                resolvedLojaId,
                request.getQuantidade(),
                authenticatedUserService.getCurrentUsuario(),
                request.getObservacao()
        );
    }

    @PostMapping("/saida")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public MovimentacaoEstoque registrarSaida(@Valid @RequestBody SaidaEstoqueRequest request) {
        Long resolvedLojaId = authenticatedUserService.resolveLojaId(request.getLojaId());
        return estoqueService.registrarSaidaMercadoria(
                request.getProdutoId(),
                resolvedLojaId,
                request.getQuantidade(),
                authenticatedUserService.getCurrentUsuario(),
                request.getObservacao()
        );
    }
}
