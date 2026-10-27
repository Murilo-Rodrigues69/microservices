package isakids.isa.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import isakids.isa.dto.CreateUsuarioRequest;
import isakids.isa.dto.UpdateUsuarioRequest;
import isakids.isa.entities.Loja;
import isakids.isa.entities.Usuario;
import isakids.isa.enums.PerfilUsuario;
import isakids.isa.repositories.LojaRepository;
import isakids.isa.repositories.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;

@Service
public class UsuarioService {

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    LojaRepository lojaRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    public Usuario buscarPorLogin(String login) {
        return usuarioRepository.findByLoginAndAtivoTrue(login)
                .orElseThrow(() -> new EntityNotFoundException("Usuario nao encontrado."));
    }

    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario nao encontrado"));
    }

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    public List<Usuario> listarAtivos() {
        return usuarioRepository.findByAtivoTrue();
    }

    @Transactional
    public Usuario criar(CreateUsuarioRequest request) {
        String login = normalizeLogin(request.getLogin());
        if (usuarioRepository.existsByLogin(login)) {
            throw new IllegalArgumentException("Usuario ja existe: " + login);
        }

        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setNome(request.getNome().trim());
        usuario.setLoja(buscarLoja(request.getLojaId()));
        usuario.setPerfil(request.getPerfil() != null ? request.getPerfil() : PerfilUsuario.CAIXA);
        usuario.setAtivo(request.getAtivo() == null ? true : request.getAtivo());
        usuario.setSenhaHash(passwordEncoder.encode(request.getSenha()));
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario atualizar(Long id, UpdateUsuarioRequest request) {
        Usuario existente = buscarPorId(id);
        String novoLogin = normalizeLogin(request.getLogin());

        usuarioRepository.findByLogin(novoLogin)
                .filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> {
                    throw new IllegalArgumentException("Usuario ja existe: " + novoLogin);
                });

        existente.setLogin(novoLogin);
        existente.setNome(request.getNome().trim());
        existente.setLoja(buscarLoja(request.getLojaId()));
        existente.setPerfil(request.getPerfil() != null ? request.getPerfil() : existente.getPerfil());
        if (request.getAtivo() != null) {
            existente.setAtivo(request.getAtivo());
        }

        if (hasText(request.getSenha())) {
            existente.setSenhaHash(passwordEncoder.encode(request.getSenha()));
        }

        return usuarioRepository.save(existente);
    }

    @Transactional
    public void deletar(Long id) {
        Usuario usuario = buscarPorId(id);
        usuario.setAtivo(false);
        usuarioRepository.save(usuario);
    }

    private String normalizeLogin(String login) {
        if (!hasText(login)) {
            throw new IllegalArgumentException("login e obrigatorio");
        }
        return login.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Loja buscarLoja(Long lojaId) {
        if (lojaId == null) {
            throw new IllegalArgumentException("lojaId e obrigatorio");
        }
        return lojaRepository.findById(lojaId)
                .orElseThrow(() -> new EntityNotFoundException("Loja nao encontrada: " + lojaId));
    }
}
