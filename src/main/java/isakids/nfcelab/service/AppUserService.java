package isakids.nfcelab.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import isakids.nfcelab.dto.CreateUserRequest;
import isakids.nfcelab.dto.UpdateUserRequest;
import isakids.nfcelab.entity.AppUserEntity;
import isakids.nfcelab.entity.UserRole;
import isakids.nfcelab.repository.AppUserRepository;
import jakarta.persistence.EntityNotFoundException;

@Service
public class AppUserService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AppUserService(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AppUserEntity findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario nao encontrado: " + id));
    }

    public List<AppUserEntity> listAll() {
        return userRepository.findAll();
    }

    public List<AppUserEntity> listActive() {
        return userRepository.findByActiveTrue();
    }

    @Transactional
    public AppUserEntity create(CreateUserRequest request) {
        String login = normalizeLogin(request.getLogin());
        if (userRepository.existsByLogin(login)) {
            throw new IllegalArgumentException("Usuario ja existe: " + login);
        }

        AppUserEntity user = new AppUserEntity();
        user.setLogin(login);
        user.setName(request.getName().trim());
        user.setRole(request.getRole() != null ? request.getRole() : UserRole.USER);
        user.setActive(request.getActive() == null || request.getActive());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        return userRepository.save(user);
    }

    @Transactional
    public AppUserEntity update(Long id, UpdateUserRequest request) {
        AppUserEntity user = findById(id);
        String login = normalizeLogin(request.getLogin());

        userRepository.findByLogin(login)
                .filter(otherUser -> !otherUser.getId().equals(id))
                .ifPresent(otherUser -> {
                    throw new IllegalArgumentException("Usuario ja existe: " + login);
                });

        user.setLogin(login);
        user.setName(request.getName().trim());
        user.setRole(request.getRole() != null ? request.getRole() : user.getRole());
        if (request.getActive() != null) {
            user.setActive(request.getActive());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        return userRepository.save(user);
    }

    @Transactional
    public void deactivate(Long id) {
        AppUserEntity user = findById(id);
        user.setActive(false);
        userRepository.save(user);
    }

    private String normalizeLogin(String login) {
        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException("login e obrigatorio");
        }
        return login.trim();
    }
}
