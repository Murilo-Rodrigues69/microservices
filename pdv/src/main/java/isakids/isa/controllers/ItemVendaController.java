package isakids.isa.controllers;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import isakids.isa.entities.ItemVenda;
import isakids.isa.services.ItemVendaService;


@RestController
@RequestMapping("/api/itens-venda")
public class ItemVendaController {


    @Autowired
    ItemVendaService itemVendaService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<ItemVenda> listarTodos() {
        return itemVendaService.listarTodos();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ItemVenda buscarPorId(@PathVariable Long id) {
        return itemVendaService.buscarPorId(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deletar(@PathVariable Long id) {
        itemVendaService.deletar(id);
    }
}
