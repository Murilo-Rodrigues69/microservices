package isakids.nfcelab.security;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import isakids.nfcelab.entity.AppUserEntity;
import isakids.nfcelab.entity.UserRole;
import isakids.nfcelab.repository.AppUserRepository;
import jakarta.servlet.http.Cookie;

@SpringBootTest(properties = {
        "app.nfce.ws-enabled=false",
        "app.security.bootstrap-admin.enabled=false",
        "app.security.jwt.secret=aXNhLW5mY2UtbGFiLXRlc3Qtand0LXNlY3JldC1jaGFuZ2UtbWUtMjAyNi0wOS0wNA==",
        "app.security.login-attempt.max-failures=3",
        "app.security.login-attempt.lockout-minutes=15",
        "spring.datasource.url=jdbc:h2:mem:auth-security-tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuthSecurityTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LoginAttemptService loginAttemptService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
                .apply(springSecurity())
                .build();
        loginAttemptService.clearAttempts();
        userRepository.deleteAll();
    }

    @Test
    void protectedApiRequiresCookieAuthentication() throws Exception {
        mockMvc.perform(get("/api/empresas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginSetsHttpOnlyCookieAndDoesNotExposeJwtInBody() throws Exception {
        createUser("admin", UserRole.ADMIN, true, "admin10");

        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "admin10"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$.user.login").value("admin"))
                .andExpect(content().string(not(containsString("jwtToken"))))
                .andExpect(content().string(not(containsString("passwordHash"))))
                .andExpect(header().string("Set-Cookie", allOf(
                        containsString("isa_nfce_auth_token="),
                        containsString("Secure"),
                        containsString("HttpOnly"),
                        containsString("SameSite=Lax"))));
    }

    @Test
    void authenticatedCookieCanReadCurrentUser() throws Exception {
        createUser("alice", UserRole.USER, true, "StrongPass1");

        MvcResult loginResult = login("alice", "StrongPass1");
        String cookieValue = extractCookieValue(loginResult.getResponse().getHeader("Set-Cookie"),
                "isa_nfce_auth_token");

        mockMvc.perform(get("/api/auth/me").cookie(new Cookie("isa_nfce_auth_token", cookieValue)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("alice"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
    }

    @Test
    void authorizationBearerHeaderIsIgnored() throws Exception {
        createUser("admin", UserRole.ADMIN, true, "admin10");

        MvcResult loginResult = login("admin", "admin10");
        String cookieValue = extractCookieValue(loginResult.getResponse().getHeader("Set-Cookie"),
                "isa_nfce_auth_token");

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + cookieValue))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutClearsAuthenticationCookie() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(header().string("Set-Cookie", allOf(
                        containsString("isa_nfce_auth_token="),
                        containsString("Max-Age=0"),
                        containsString("Secure"),
                        containsString("HttpOnly"),
                        containsString("SameSite=Lax"))));
    }

    @Test
    void repeatedInvalidLoginsAreTemporarilyBlocked() throws Exception {
        createUser("admin", UserRole.ADMIN, true, "admin10");

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/auth/signin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "username": "admin",
                                      "password": "wrong-password"
                                    }
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "admin10"
                                }
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    void corsAllowsCredentialsFromLocalFrontend() throws Exception {
        mockMvc.perform(options("/api/auth/signin")
                        .header("Origin", "http://localhost:5000")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5000"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    private MvcResult login(String username, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
    }

    private AppUserEntity createUser(String login, UserRole role, boolean active, String password) {
        AppUserEntity user = new AppUserEntity();
        user.setLogin(login);
        user.setName("Usuario " + login);
        user.setRole(role);
        user.setActive(active);
        user.setPasswordHash(passwordEncoder.encode(password));
        return userRepository.save(user);
    }

    private String extractCookieValue(String setCookieHeader, String cookieName) {
        String marker = cookieName + "=";
        int start = setCookieHeader.indexOf(marker);
        if (start < 0) {
            throw new IllegalStateException("Cookie nao encontrado na resposta: " + cookieName);
        }

        int valueStart = start + marker.length();
        int valueEnd = setCookieHeader.indexOf(';', valueStart);
        if (valueEnd < 0) {
            throw new IllegalStateException("Valor invalido para o cookie: " + cookieName);
        }

        return setCookieHeader.substring(valueStart, valueEnd);
    }
}
