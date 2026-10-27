package isakids.isa.dto;

import isakids.isa.enums.PerfilUsuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateUsuarioRequest {

    @NotBlank
    private String login;

    @NotBlank
    private String nome;

    @Size(min = 8, max = 72)
    private String senha;

    @NotNull
    private Long lojaId;
    private PerfilUsuario perfil;
    private Boolean ativo;

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

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public Long getLojaId() {
        return lojaId;
    }

    public void setLojaId(Long lojaId) {
        this.lojaId = lojaId;
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
}
