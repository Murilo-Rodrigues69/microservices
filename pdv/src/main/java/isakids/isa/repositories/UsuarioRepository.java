package isakids.isa.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import isakids.isa.entities.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByLogin(String login);
    Optional<Usuario> findByLoginAndAtivoTrue(String login);
    Optional<Usuario> findByIdAndAtivoTrue(Long id);
    boolean existsByLogin(String login);
    List<Usuario> findByAtivoTrue();

}
