package isakids.nfcelab.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import isakids.nfcelab.entity.AppUserEntity;

public interface AppUserRepository extends JpaRepository<AppUserEntity, Long> {

    Optional<AppUserEntity> findByLogin(String login);

    Optional<AppUserEntity> findByLoginAndActiveTrue(String login);

    boolean existsByLogin(String login);

    List<AppUserEntity> findByActiveTrue();
}
