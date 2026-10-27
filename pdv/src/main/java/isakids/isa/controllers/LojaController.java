package isakids.isa.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import isakids.isa.entities.Loja;
import isakids.isa.services.LojaService;

@RestController
@RequestMapping("/api/lojas")
public class LojaController {

    @Autowired
    private LojaService lojaService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<Loja> listarTodas() {
        return lojaService.listarTodas();
    }

    @GetMapping("/ativas")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Loja> listarAtivas() {
        return lojaService.listarAtivas();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Loja buscarPorId(@PathVariable Long id) {
        return lojaService.buscarPorId(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Loja criar(@RequestBody Loja loja) {
        return lojaService.salvar(loja);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Loja atualizar(@PathVariable Long id, @RequestBody Loja atualizada) {
        Loja existente = lojaService.buscarPorId(id);
        existente.setCodigo(atualizada.getCodigo());
        existente.setNome(atualizada.getNome());
        existente.setCnpj(atualizada.getCnpj());
        existente.setInscricaoEstadual(atualizada.getInscricaoEstadual());
        existente.setAtivo(atualizada.getAtivo());
        return lojaService.salvar(existente);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deletar(@PathVariable Long id) {
        lojaService.desativar(id);
    }
}
