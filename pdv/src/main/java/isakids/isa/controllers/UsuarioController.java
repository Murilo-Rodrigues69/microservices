package isakids.isa.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import isakids.isa.dto.CreateUsuarioRequest;
import isakids.isa.dto.UsuarioAutenticadoResponse;
import isakids.isa.dto.UpdateUsuarioRequest;
import isakids.isa.dto.UsuarioResponse;
import isakids.isa.services.AuthenticatedUserService;
import isakids.isa.services.UsuarioService;
import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    UsuarioService usuarioService;

    @Autowired
    AuthenticatedUserService authenticatedUserService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UsuarioResponse> listarTodos() {
        return usuarioService.listarTodos().stream()
                .map(UsuarioResponse::fromEntity)
                .toList();
    }

    @GetMapping("/me")
    public UsuarioAutenticadoResponse usuarioAutenticado(Authentication authentication) {
        return UsuarioAutenticadoResponse.fromEntity(
                authenticatedUserService.getCurrentUsuario(),
                authentication.getAuthorities());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioResponse buscarPorId(@PathVariable Long id) {
        return UsuarioResponse.fromEntity(usuarioService.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioResponse criar(@Valid @RequestBody CreateUsuarioRequest usuario) {
        return UsuarioResponse.fromEntity(usuarioService.criar(usuario));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioResponse atualizar(@PathVariable Long id, @Valid @RequestBody UpdateUsuarioRequest atualizado) {
        return UsuarioResponse.fromEntity(usuarioService.atualizar(id, atualizado));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deletar(@PathVariable Long id) {
        usuarioService.deletar(id);
    }

    @GetMapping("/ativos")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UsuarioResponse> listarAtivos() {
        return usuarioService.listarAtivos().stream()
                .map(UsuarioResponse::fromEntity)
                .toList();
    }
}
