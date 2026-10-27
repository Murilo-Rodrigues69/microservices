package isakids.isa.jwt;

import java.util.List;

import isakids.isa.dto.UsuarioAutenticadoResponse;

public class LoginResponse {
    private String jwtToken;

    private String username;
    private List<String> roles;
    private UsuarioAutenticadoResponse usuario;

    public LoginResponse(String username, List<String> roles, String jwtToken, UsuarioAutenticadoResponse usuario) {
        this.username = username;
        this.roles = roles;
        this.jwtToken = jwtToken;
        this.usuario = usuario;
    }

    public String getJwtToken() {
        return jwtToken;
    }

    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
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

    public UsuarioAutenticadoResponse getUsuario() {
        return usuario;
    }

    public void setUsuario(UsuarioAutenticadoResponse usuario) {
        this.usuario = usuario;
    }
}
