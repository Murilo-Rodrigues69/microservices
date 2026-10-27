package isakids.isa.services;

import java.util.Objects;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import isakids.isa.entities.Usuario;
import isakids.isa.entities.Venda;
import isakids.isa.repositories.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;

@Service
public class AuthenticatedUserService {

    private final UsuarioRepository usuarioRepository;

    public AuthenticatedUserService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario resolveUsuarioOperador(Long requestedUsuarioId) {
        if (isCurrentUserAdmin()) {
            if (requestedUsuarioId != null) {
                return buscarUsuarioAtivoPorId(requestedUsuarioId);
            }
            return getCurrentUsuario();
        }

        Usuario currentUsuario = getCurrentUsuario();
        if (requestedUsuarioId != null && !Objects.equals(currentUsuario.getId(), requestedUsuarioId)) {
            throw new AccessDeniedException("Nao e permitido operar em nome de outro usuario.");
        }
        return currentUsuario;
    }

    public Long resolveLojaId(Long requestedLojaId) {
        if (isCurrentUserAdmin()) {
            return requestedLojaId;
        }

        Usuario currentUsuario = getCurrentUsuario();
        if (currentUsuario.getLoja() == null || currentUsuario.getLoja().getId() == null) {
            throw new IllegalStateException("Usuario autenticado sem loja vinculada.");
        }

        Long currentLojaId = currentUsuario.getLoja().getId();
        if (requestedLojaId != null && !Objects.equals(currentLojaId, requestedLojaId)) {
            throw new AccessDeniedException("Nao e permitido acessar dados de outra loja.");
        }
        return currentLojaId;
    }

    public void checkCanAccessVenda(Venda venda) {
        if (isCurrentUserAdmin()) {
            return;
        }

        Usuario currentUsuario = getCurrentUsuario();
        Long currentUsuarioId = currentUsuario.getId();
        Long vendaUsuarioId = venda.getUsuario() != null ? venda.getUsuario().getId() : null;
        Long currentLojaId = currentUsuario.getLoja() != null ? currentUsuario.getLoja().getId() : null;
        Long vendaLojaId = venda.getLoja() != null ? venda.getLoja().getId() : null;

        if (!Objects.equals(currentUsuarioId, vendaUsuarioId) || !Objects.equals(currentLojaId, vendaLojaId)) {
            throw new AccessDeniedException("Nao e permitido acessar esta venda.");
        }
    }

    public Usuario getCurrentUsuario() {
        String username = getRequiredAuthentication().getName();
        return usuarioRepository.findByLoginAndAtivoTrue(username)
                .orElseThrow(() -> new EntityNotFoundException("Usuario autenticado nao encontrado ou inativo."));
    }

    public boolean isCurrentUserAdmin() {
        return getRequiredAuthentication().getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private Usuario buscarUsuarioAtivoPorId(Long usuarioId) {
        return usuarioRepository.findByIdAndAtivoTrue(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario nao encontrado ou inativo: " + usuarioId));
    }

    private Authentication getRequiredAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Autenticacao obrigatoria.");
        }
        return authentication;
    }
}
