package isakids.isa.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import isakids.isa.dto.UsuarioAutenticadoResponse;
import isakids.isa.jwt.JwtUtils;
import isakids.isa.jwt.LoginRequest;
import isakids.isa.jwt.LoginResponse;
import isakids.isa.services.AuthenticatedUserService;
import jakarta.validation.Valid;

@RestController
@Validated
public class JwtController {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
        } catch (AuthenticationException exception) {
            Map<String, Object> map = new HashMap<>();
            map.put("message", "Bad credentials");
            map.put("status", false);
            return new ResponseEntity<>(map, HttpStatus.UNAUTHORIZED);
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        String jwtToken = jwtUtils.generateTokenFromUsername(userDetails);
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        UsuarioAutenticadoResponse usuario = UsuarioAutenticadoResponse.fromEntity(
                authenticatedUserService.getCurrentUsuario(),
                userDetails.getAuthorities());

        LoginResponse response = new LoginResponse(userDetails.getUsername(), roles, jwtToken, usuario);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtUtils.createAuthCookie(jwtToken).toString())
                .body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        SecurityContextHolder.clearContext();

        Map<String, Object> map = new HashMap<>();
        map.put("status", true);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtUtils.createLogoutCookie().toString())
                .body(map);
    }
}
