package isakids.isa.dto;

import isakids.isa.entities.Usuario;
import isakids.isa.enums.PerfilUsuario;

public class UsuarioResponse {

    private Long id;
    private String login;
    private String nome;
    private PerfilUsuario perfil;
    private Boolean ativo;
    private Long lojaId;
    private String lojaNome;

    public static UsuarioResponse fromEntity(Usuario usuario) {
        UsuarioResponse response = new UsuarioResponse();
        response.setId(usuario.getId());
        response.setLogin(usuario.getLogin());
        response.setNome(usuario.getNome());
        response.setPerfil(usuario.getPerfil());
        response.setAtivo(usuario.getAtivo());
        if (usuario.getLoja() != null) {
            response.setLojaId(usuario.getLoja().getId());
            response.setLojaNome(usuario.getLoja().getNome());
        }
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public PerfilUsuario getPerfil() {
        return perfil;
    }

    public void setPerfil(PerfilUsuario perfil) {
        this.perfil = perfil;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public Long getLojaId() {
        return lojaId;
    }

    public void setLojaId(Long lojaId) {
        this.lojaId = lojaId;
    }

    public String getLojaNome() {
        return lojaNome;
    }

    public void setLojaNome(String lojaNome) {
        this.lojaNome = lojaNome;
    }
}
