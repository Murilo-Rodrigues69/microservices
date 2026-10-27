package isakids.isa;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import isakids.isa.entities.Loja;
import isakids.isa.repositories.LojaRepository;
import jakarta.servlet.http.Cookie;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class BootstrapUsersIntegrationTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private LojaRepository lojaRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void bootstrapAdminUserCanAuthenticateAndAccessProtectedEndpoints() throws Exception {
        Loja loja = criarLoja("MATRIZ", "Loja Matriz");

        MvcResult loginResult = mockMvc.perform(post("/signin")
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
                .andExpect(jsonPath("$.jwtToken").isNotEmpty())
                .andExpect(jsonPath("$.usuario.login").value("admin"))
                .andExpect(jsonPath("$.usuario.perfil").value("ADMIN"))
                .andExpect(jsonPath("$.usuario.lojaId").isNumber())
                .andExpect(header().string("Set-Cookie", Matchers.allOf(
                        Matchers.containsString("isa_auth_token="),
                        Matchers.containsString("HttpOnly"),
                        Matchers.containsString("SameSite=Lax"))))
                .andReturn();

        String authCookieValue = extractCookieValue(loginResult.getResponse().getHeader("Set-Cookie"), "isa_auth_token");

        mockMvc.perform(post("/api/usuarios")
                        .cookie(new Cookie("isa_auth_token", authCookieValue))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "login": "vendedor-token",
                                  "nome": "Vendedor Token",
                                  "senha": "StrongPass1",
                                  "lojaId": %d,
                                  "perfil": "CAIXA",
                                  "ativo": true
                                }
                                """.formatted(loja.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("vendedor-token"))
                .andExpect(jsonPath("$.perfil").value("CAIXA"))
                .andExpect(jsonPath("$.lojaId").value(loja.getId()));

        mockMvc.perform(post("/api/lojas")
                        .cookie(new Cookie("isa_auth_token", authCookieValue))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigo": "LOJA-NOVA",
                                  "nome": "Loja Nova",
                                  "ativo": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("LOJA-NOVA"))
                .andExpect(jsonPath("$.nome").value("Loja Nova"))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    private Loja criarLoja(String codigo, String nome) {
        Loja loja = new Loja();
        loja.setCodigo(codigo);
        loja.setNome(nome);
        loja.setAtivo(true);
        return lojaRepository.save(loja);
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
