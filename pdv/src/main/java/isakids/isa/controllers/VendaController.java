package isakids.isa.controllers;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import isakids.isa.dto.ResumoVendaDTO;
import isakids.isa.entities.Venda;
import isakids.isa.enums.StatusVenda;
import isakids.isa.services.VendaService;

@RestController
@RequestMapping("/api/vendas")
public class VendaController {

    @Autowired
    VendaService vendaService;

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Venda buscarPorId(@PathVariable Long id) {
        return vendaService.buscarPorId(id);
    }

    @GetMapping("/resumo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResumoVendaDTO resumoPeriodo(
        @RequestParam(required = false) Long lojaId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim
    ) {
        return vendaService.resumoPeriodo(inicio, fim, lojaId);
    }

    @GetMapping("/resumo/hoje")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResumoVendaDTO resumoHoje(@RequestParam(required = false) Long lojaId) {
        LocalDate hoje = LocalDate.now();
        return vendaService.resumoPeriodo(hoje, hoje, lojaId);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<Venda> listarTodas(@RequestParam(required = false) Long lojaId) {
        return vendaService.listarTodas(lojaId);
    }

    @GetMapping("/periodo")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Venda> listarPeriodo(
        @RequestParam(required = false) Long lojaId,
        @RequestParam String inicio,
        @RequestParam String fim,
        @RequestParam(required = false, defaultValue = "CONCLUIDA") StatusVenda status
    ) {
        return vendaService.listarPorPeriodo(inicio, fim, status, lojaId);
    }

    @GetMapping("/hoje")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public List<Venda> listarVendasHoje(@RequestParam(required = false) Long lojaId) {
      return vendaService.listarVendasHoje(lojaId);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Venda atualizar(@PathVariable Long id, @RequestBody Venda venda) {
        return vendaService.atualizar(id, venda);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deletar(@PathVariable Long id) {
        vendaService.deletar(id);
    }
}
