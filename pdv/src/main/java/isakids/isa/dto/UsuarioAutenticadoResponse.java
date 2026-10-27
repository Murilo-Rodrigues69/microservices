package isakids.isa.dto;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;

import isakids.isa.entities.Usuario;

public class UsuarioAutenticadoResponse extends UsuarioResponse {

    private List<String> roles;

    public static UsuarioAutenticadoResponse fromEntity(Usuario usuario,
            Collection<? extends GrantedAuthority> authorities) {
        UsuarioAutenticadoResponse response = new UsuarioAutenticadoResponse();
        response.setId(usuario.getId());
        response.setLogin(usuario.getLogin());
        response.setNome(usuario.getNome());
        response.setPerfil(usuario.getPerfil());
        response.setAtivo(usuario.getAtivo());
        if (usuario.getLoja() != null) {
            response.setLojaId(usuario.getLoja().getId());
            response.setLojaNome(usuario.getLoja().getNome());
        }
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
