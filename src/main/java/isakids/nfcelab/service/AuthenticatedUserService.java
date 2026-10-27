package isakids.nfcelab.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import isakids.nfcelab.entity.AppUserEntity;
import isakids.nfcelab.repository.AppUserRepository;
import jakarta.persistence.EntityNotFoundException;

@Service
public class AuthenticatedUserService {

    private final AppUserRepository userRepository;

    public AuthenticatedUserService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AppUserEntity getCurrentUser() {
        String username = getRequiredAuthentication().getName();
        return userRepository.findByLoginAndActiveTrue(username)
                .orElseThrow(() -> new EntityNotFoundException("Usuario autenticado nao encontrado ou inativo."));
    }

    public boolean isCurrentUserAdmin() {
        return getRequiredAuthentication().getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
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
