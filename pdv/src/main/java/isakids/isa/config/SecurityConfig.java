package isakids.isa.config;

import static org.springframework.security.config.Customizer.withDefaults;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import isakids.isa.entities.Loja;
import isakids.isa.entities.Usuario;
import isakids.isa.enums.PerfilUsuario;
import isakids.isa.jwt.AuthEntryPointJwt;
import isakids.isa.jwt.AuthTokenFilter;
import isakids.isa.repositories.LojaRepository;
import isakids.isa.repositories.UsuarioRepository;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;

    @Value("${app.security.bootstrap-admin.login:admin}")
    private String bootstrapAdminLogin;

    @Value("${app.cors.allowed-origins:}")
    private String allowedOrigins;

    @Value("${app.security.test-seed-user-password:}")
    private String testSeedUserPassword;

    @Value("${app.security.test-seed-admin-password:}")
    private String testSeedAdminPassword;

    @Value("${app.security.bootstrap-admin.name:Administrador}")
    private String bootstrapAdminName;

    @Value("${app.security.bootstrap-admin.password:}")
    private String bootstrapAdminPassword;

    @Value("${app.security.bootstrap-admin.store-code:MATRIZ}")
    private String bootstrapAdminStoreCode;

    @Value("${app.security.bootstrap-admin.store-name:Loja Matriz}")
    private String bootstrapAdminStoreName;

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

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
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(withDefaults())
            .csrf(csrf -> csrf.disable())
            .logout(logout -> logout.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedHandler))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers(HttpMethod.POST, "/signin", "/logout", "/sign", "/sign/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/whatsapp/webhook").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/whatsapp/webhook").permitAll()
                .requestMatchers(
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/v3/api-docs.yaml"
                    ).permitAll()
                .anyRequest().authenticated()
            );

        http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));
        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean(name = "corsConfigurationSource")
    @Profile("test")
    public CorsConfigurationSource testCorsConfigurationSource() {
        return buildCorsConfigurationSource(List.of("http://localhost:*", "http://127.0.0.1:*"));
    }

    @Bean(name = "corsConfigurationSource")
    @Profile({ "default", "dev" })
    public CorsConfigurationSource localCorsConfigurationSource() {
        return buildCorsConfigurationSource(List.of("http://localhost:*", "http://127.0.0.1:*"));
    }

    @Bean(name = "corsConfigurationSource")
    @Profile("prod")
    public CorsConfigurationSource restrictedCorsConfigurationSource() {
        return buildCorsConfigurationSource(java.util.Arrays.stream(allowedOrigins.split(",")).map(String::trim).filter(origin -> !origin.isEmpty()).toList());
    }

    private CorsConfigurationSource buildCorsConfigurationSource(List<String> allowedOriginPatterns) {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(allowedOriginPatterns);
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setExposedHeaders(List.of("Authorization"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration builder) throws Exception {
        return builder.getAuthenticationManager();
    }

    @Bean
    @Profile("test")
    @ConditionalOnProperty(name = "app.security.seed-default-users", havingValue = "true")
    public org.springframework.boot.CommandLineRunner initData(UsuarioRepository usuarioRepository,
            LojaRepository lojaRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            Loja lojaPadrao = lojaRepository.findByCodigo("SEED-DEFAULT")
                    .orElseGet(() -> {
                        Loja loja = new Loja();
                        loja.setCodigo("SEED-DEFAULT");
                        loja.setNome("Loja Padrao");
                        loja.setAtivo(true);
                        return lojaRepository.save(loja);
                    });

            createSeedUser(usuarioRepository, passwordEncoder, lojaPadrao, "user1", "Usuario Padrao",
                    PerfilUsuario.CAIXA, testSeedUserPassword, true);
            createSeedUser(usuarioRepository, passwordEncoder, lojaPadrao, "admin", "Administrador",
                    PerfilUsuario.ADMIN, testSeedAdminPassword, true);
        };
    }

    @Bean
    @Profile("!test")
    @ConditionalOnProperty(name = "app.security.bootstrap-admin.enabled", havingValue = "true")
    public org.springframework.boot.CommandLineRunner bootstrapAdminData(UsuarioRepository usuarioRepository,
            LojaRepository lojaRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (bootstrapAdminPassword == null || bootstrapAdminPassword.isBlank()) {
                throw new IllegalStateException(
                        "app.security.bootstrap-admin.password precisa estar configurada quando o bootstrap admin estiver habilitado.");
            }

            Loja lojaBootstrap = lojaRepository.findByCodigo(bootstrapAdminStoreCode)
                    .orElseGet(() -> {
                        Loja loja = new Loja();
                        loja.setCodigo(bootstrapAdminStoreCode);
                        loja.setNome(bootstrapAdminStoreName);
                        loja.setAtivo(true);
                        return lojaRepository.save(loja);
                    });

            createBootstrapAdminIfMissing(
                    usuarioRepository,
                    passwordEncoder,
                    lojaBootstrap,
                    bootstrapAdminLogin,
                    bootstrapAdminName,
                    bootstrapAdminPassword);
        };
    }

    private void createSeedUser(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, Loja loja,
            String login, String nome, PerfilUsuario perfil, String senha, boolean ativo) {
        Usuario usuario = usuarioRepository.findByLogin(login).orElseGet(Usuario::new);
        usuario.setLogin(login);
        usuario.setNome(nome);
        usuario.setPerfil(perfil);
        usuario.setAtivo(ativo);
        usuario.setSenhaHash(passwordEncoder.encode(senha));
        if (usuario.getLoja() == null) {
            usuario.setLoja(loja);
        }
        usuarioRepository.save(usuario);
    }

    private void createBootstrapAdminIfMissing(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
            Loja loja, String login, String nome, String senha) {
        if (usuarioRepository.findByLogin(login).isPresent()) {
            return;
        }

        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setNome(nome);
        usuario.setPerfil(PerfilUsuario.ADMIN);
        usuario.setAtivo(true);
        usuario.setSenhaHash(passwordEncoder.encode(senha));
        usuario.setLoja(loja);
        usuarioRepository.save(usuario);
    }
}
