package isakids.isa.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import isakids.isa.entities.Loja;
import isakids.isa.repositories.LojaRepository;
import jakarta.persistence.EntityNotFoundException;

@Service
public class LojaService {

    @Autowired
    private LojaRepository lojaRepository;

    public Loja buscarPorId(Long id) {
        return lojaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Loja nao encontrada: " + id));
    }

    public List<Loja> listarTodas() {
        return lojaRepository.findAll();
    }

    public List<Loja> listarAtivas() {
        return lojaRepository.findByAtivoTrue();
    }

    @Transactional
    public Loja salvar(Loja loja) {
        if (loja.getCodigo() == null || loja.getCodigo().isBlank()) {
            throw new IllegalArgumentException("codigo da loja e obrigatorio");
        }
        if (loja.getNome() == null || loja.getNome().isBlank()) {
            throw new IllegalArgumentException("nome da loja e obrigatorio");
        }

        if (loja.getAtivo() == null) {
            loja.setAtivo(true);
        }

        return lojaRepository.save(loja);
    }

    @Transactional
    public void desativar(Long id) {
        Loja loja = buscarPorId(id);
        loja.setAtivo(false);
        lojaRepository.save(loja);
    }
}
