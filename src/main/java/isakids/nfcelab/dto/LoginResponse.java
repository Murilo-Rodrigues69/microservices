package isakids.nfcelab.dto;

import java.util.List;

public class LoginResponse {

    private String username;
    private List<String> roles;
    private AuthenticatedUserResponse user;

    public LoginResponse(String username, List<String> roles, AuthenticatedUserResponse user) {
        this.username = username;
        this.roles = roles;
        this.user = user;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public AuthenticatedUserResponse getUser() {
        return user;
    }

    public void setUser(AuthenticatedUserResponse user) {
        this.user = user;
    }
}
