package isakids.nfcelab.service;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import isakids.nfcelab.entity.AppUserEntity;
import isakids.nfcelab.entity.UserRole;
import isakids.nfcelab.repository.AppUserRepository;

@Service
public class UserAuthenticationService implements UserDetailsService {

    private final AppUserRepository userRepository;

    public UserAuthenticationService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String normalizedUsername = normalizeUsername(username);
        AppUserEntity appUser = userRepository.findByLogin(normalizedUsername)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado: " + normalizedUsername));

        if (appUser.getPasswordHash() == null || appUser.getPasswordHash().isBlank()) {
            throw new UsernameNotFoundException("Usuario sem credencial valida: " + normalizedUsername);
        }

        return User.withUsername(appUser.getLogin())
                .password(appUser.getPasswordHash())
                .authorities(authoritiesForRole(appUser.getRole()).stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList())
                .disabled(!appUser.isActive())
                .build();
    }

    public List<String> authoritiesForRole(UserRole role) {
        UserRole resolvedRole = role != null ? role : UserRole.USER;
        return switch (resolvedRole) {
        case ADMIN -> List.of("ROLE_ADMIN");
        case USER -> List.of("ROLE_USER");
        };
    }

    private String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new UsernameNotFoundException("Username invalido.");
        }
        return username.trim();
    }
}
