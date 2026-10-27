package isakids.isa.services;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import isakids.isa.entities.Usuario;
import isakids.isa.enums.PerfilUsuario;
import isakids.isa.repositories.UsuarioRepository;

@Service
public class UsuarioAuthenticationService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioAuthenticationService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String normalizedUsername = normalizeUsername(username);
        Usuario usuario = usuarioRepository.findByLogin(normalizedUsername)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado: " + normalizedUsername));

        if (usuario.getSenhaHash() == null || usuario.getSenhaHash().isBlank()) {
            throw new UsernameNotFoundException("Usuario sem credencial valida: " + normalizedUsername);
        }

        return User.withUsername(usuario.getLogin())
                .password(usuario.getSenhaHash())
                .authorities(authoritiesForPerfil(usuario.getPerfil()).stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList())
                .disabled(!Boolean.TRUE.equals(usuario.getAtivo()))
                .build();
    }

    public List<String> authoritiesForPerfil(PerfilUsuario perfil) {
        PerfilUsuario resolvedPerfil = perfil != null ? perfil : PerfilUsuario.CAIXA;
        return switch (resolvedPerfil) {
        case ADMIN -> List.of("ROLE_ADMIN");
        case CAIXA, GERENTE -> List.of("ROLE_USER");
        };
    }

    private String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new UsernameNotFoundException("Username invalido.");
        }
        return username.trim();
    }
}
