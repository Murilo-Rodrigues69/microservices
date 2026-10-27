package isakids.nfcelab.config;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import isakids.nfcelab.entity.AppUserEntity;
import isakids.nfcelab.entity.UserRole;
import isakids.nfcelab.repository.AppUserRepository;
import isakids.nfcelab.security.AuthEntryPointJwt;
import isakids.nfcelab.security.AuthTokenFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${app.security.bootstrap-admin.login:admin}")
    private String bootstrapAdminLogin;

    @Value("${app.security.bootstrap-admin.name:Administrador}")
    private String bootstrapAdminName;

    @Value("${app.security.bootstrap-admin.password:}")
    private String bootstrapAdminPassword;

    @Bean
    @Order(1)
    @ConditionalOnProperty(name = "spring.h2.console.enabled", havingValue = "true")
    SecurityFilterChain h2ConsoleSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/h2-console/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));

        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http, AuthEntryPointJwt unauthorizedHandler,
            AuthTokenFilter authTokenFilter) throws Exception {
        http
                .cors(withDefaults())
                .csrf(csrf -> csrf.disable())
                .logout(logout -> logout.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/", "/index.html", "/assets/**", "/favicon.ico").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/signin", "/api/auth/logout").permitAll()
                        .anyRequest().authenticated());

        http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));
        http.addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    @Profile("!test")
    @ConditionalOnProperty(name = "app.security.bootstrap-admin.enabled", havingValue = "true")
    public CommandLineRunner bootstrapAdminUser(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String login = normalizeBootstrapValue(bootstrapAdminLogin, "app.security.bootstrap-admin.login");
            String name = normalizeBootstrapValue(bootstrapAdminName, "app.security.bootstrap-admin.name");
            String password = normalizeBootstrapValue(bootstrapAdminPassword, "app.security.bootstrap-admin.password");

            if (userRepository.findByLogin(login).isPresent()) {
                return;
            }

            AppUserEntity user = new AppUserEntity();
            user.setLogin(login);
            user.setName(name);
            user.setRole(UserRole.ADMIN);
            user.setActive(true);
            user.setPasswordHash(passwordEncoder.encode(password));
            userRepository.save(user);
        };
    }

    private String normalizeBootstrapValue(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(propertyName + " precisa estar configurada.");
        }
        return value.trim();
    }
}
