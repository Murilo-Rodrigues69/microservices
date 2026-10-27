package isakids.nfcelab.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import isakids.nfcelab.dto.AuthenticatedUserResponse;
import isakids.nfcelab.dto.LoginRequest;
import isakids.nfcelab.dto.LoginResponse;
import isakids.nfcelab.security.JwtUtils;
import isakids.nfcelab.security.LoginAttemptService;
import isakids.nfcelab.service.AuthenticatedUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtUtils jwtUtils;
    private final LoginAttemptService loginAttemptService;
    private final AuthenticationManager authenticationManager;
    private final AuthenticatedUserService authenticatedUserService;

    public AuthController(JwtUtils jwtUtils, LoginAttemptService loginAttemptService, AuthenticationManager authenticationManager,
            AuthenticatedUserService authenticatedUserService) {
        this.jwtUtils = jwtUtils;
        this.loginAttemptService = loginAttemptService;
        this.authenticationManager = authenticationManager;
        this.authenticatedUserService = authenticatedUserService;
    }

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        if (loginAttemptService.isBlocked(loginRequest.getUsername(), request)) {
            long retryAfterSeconds = Math.max(1, loginAttemptService.remainingLockout(
                    loginRequest.getUsername(), request).toSeconds());
            Map<String, Object> body = new HashMap<>();
            body.put("message", "Muitas tentativas invalidas. Tente novamente mais tarde.");
            body.put("status", false);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds))
                    .body(body);
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
        } catch (AuthenticationException exception) {
            loginAttemptService.recordFailure(loginRequest.getUsername(), request);
            Map<String, Object> body = new HashMap<>();
            body.put("message", "Credenciais invalidas.");
            body.put("status", false);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
        }

        loginAttemptService.recordSuccess(loginRequest.getUsername(), request);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String jwtToken = jwtUtils.generateTokenFromUsername(userDetails);
        List<String> roles = userDetails.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .toList();

        AuthenticatedUserResponse user = AuthenticatedUserResponse.fromEntity(
                authenticatedUserService.getCurrentUser(),
                userDetails.getAuthorities());
        LoginResponse response = new LoginResponse(userDetails.getUsername(), roles, user);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtUtils.createAuthCookie(jwtToken).toString())
                .body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        SecurityContextHolder.clearContext();

        Map<String, Object> body = new HashMap<>();
        body.put("status", true);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtUtils.createLogoutCookie().toString())
                .body(body);
    }

    @GetMapping("/me")
    public AuthenticatedUserResponse currentUser(Authentication authentication) {
        return AuthenticatedUserResponse.fromEntity(
                authenticatedUserService.getCurrentUser(),
                authentication.getAuthorities());
    }
}
