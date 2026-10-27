package isakids.isa.services;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import isakids.isa.entities.ItemVenda;
import isakids.isa.repositories.ItemVendaRepository;
import jakarta.persistence.EntityNotFoundException;

@Service
public class ItemVendaService {

    @Autowired
    ItemVendaRepository repository;

    public ItemVendaService(ItemVendaRepository repository) {
        this.repository = repository;
    }

    public List<ItemVenda> listarTodos() {
        return repository.findAll();
    }

    public ItemVenda buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Item não encontrado"));
    }

    public void deletar(Long id) {
        ItemVenda item = buscarPorId(id);
        repository.delete(item);
    }
}
