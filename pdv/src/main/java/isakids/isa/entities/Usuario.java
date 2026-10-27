package isakids.isa.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;

import isakids.isa.enums.PerfilUsuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String login;

    @Column(nullable = false)
    @JsonIgnore
    private String senhaHash;

    @Column(nullable = false)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PerfilUsuario perfil;

    @Column(nullable = false)
    private Boolean ativo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loja_id", nullable = false)
    private Loja loja;

    public Usuario() {
    }

    public Usuario(Long id, String login, String senhaHash, String nome, PerfilUsuario perfil, Boolean ativo, Loja loja) {
        this.id = id;
        this.login = login;
        this.senhaHash = senhaHash;
        this.nome = nome;
        this.perfil = perfil;
        this.ativo = ativo;
        this.loja = loja;
    }

    public Long getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
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

    public Loja getLoja() {
        return loja;
    }

    public void setLoja(Loja loja) {
        this.loja = loja;
    }
}
