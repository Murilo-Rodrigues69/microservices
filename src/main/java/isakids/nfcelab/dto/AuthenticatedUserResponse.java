package isakids.nfcelab.dto;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;

import isakids.nfcelab.entity.AppUserEntity;

public class AuthenticatedUserResponse extends UserResponse {

    private List<String> roles;

    public static AuthenticatedUserResponse fromEntity(AppUserEntity user,
            Collection<? extends GrantedAuthority> authorities) {
        AuthenticatedUserResponse response = new AuthenticatedUserResponse();
        response.setId(user.getId());
        response.setLogin(user.getLogin());
        response.setName(user.getName());
        response.setRole(user.getRole());
        response.setActive(user.isActive());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        response.setRoles(authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .toList());
        return response;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
}
