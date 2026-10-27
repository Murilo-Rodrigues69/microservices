package isakids.isa;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import isakids.isa.entities.Loja;
import isakids.isa.entities.Produto;
import isakids.isa.entities.ProdutoLoja;
import isakids.isa.entities.Usuario;
import isakids.isa.entities.Venda;
import isakids.isa.enums.PerfilUsuario;
import isakids.isa.enums.StatusVenda;
import isakids.isa.repositories.LojaRepository;
import isakids.isa.repositories.MovimentacaoEstoqueRepository;
import isakids.isa.repositories.ProdutoLojaRepository;
import isakids.isa.repositories.ProdutoRepository;
import isakids.isa.repositories.UsuarioRepository;
import isakids.isa.repositories.VendaRepository;
import jakarta.servlet.http.Cookie;

@SpringBootTest(properties = "app.integracoes.whatsapp.verify-token=test-token")
@ActiveProfiles("test")
class SecurityWebLayerTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private LojaRepository lojaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private VendaRepository vendaRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private ProdutoLojaRepository produtoLojaRepository;

    @Autowired
    private MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;

    private MockMvc mockMvc;
    private Loja lojaPadrao;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
                .apply(springSecurity())
                .build();
        movimentacaoEstoqueRepository.deleteAll();
        vendaRepository.deleteAll();
        produtoLojaRepository.deleteAll();
        produtoRepository.deleteAll();
        usuarioRepository.deleteAll();
        lojaRepository.deleteAll();
        lojaPadrao = criarLoja("LOJA-TESTE", "Loja Teste");
    }

    @Test
    void adminEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whatsappWebhookVerificationEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/api/whatsapp/webhook")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", "test-token")
                        .param("hub.challenge", "abc123"))
                .andExpect(status().isOk())
                .andExpect(content().string("abc123"));
    }

    @Test
    void whatsappWebhookReceiveEndpointIsPublic() throws Exception {
        mockMvc.perform(post("/api/whatsapp/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "object": "whatsapp_business_account",
                                  "entry": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received").value(true))
                .andExpect(jsonPath("$.entryCount").value(0));
    }

    @Test
    void userRoleCannotAccessAdminUserEndpoint() throws Exception {
        mockMvc.perform(get("/api/usuarios").with(user("caixa").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminUserListingDoesNotExposePasswordHash() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setLogin("alice");
        usuario.setNome("Alice");
        usuario.setPerfil(PerfilUsuario.ADMIN);
        usuario.setAtivo(true);
        usuario.setSenhaHash(passwordEncoder.encode("StrongPass1"));
        usuario.setLoja(lojaPadrao);
        usuarioRepository.save(usuario);

        mockMvc.perform(get("/api/usuarios").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].login").value("alice"))
                .andExpect(content().string(Matchers.not(Matchers.containsString("senhaHash"))));
    }

    @Test
    void adminCanCreateRealUserAndNewCredentialCanAuthenticate() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "login": "vendedor1",
                                  "nome": "Vendedor 1",
                                  "senha": "StrongPass1",
                                  "lojaId": %d,
                                  "perfil": "CAIXA",
                                  "ativo": true
                                }
                                """.formatted(lojaPadrao.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("vendedor1"))
                .andExpect(jsonPath("$.nome").value("Vendedor 1"))
                .andExpect(jsonPath("$.perfil").value("CAIXA"))
                .andExpect(jsonPath("$.lojaId").value(lojaPadrao.getId()))
                .andExpect(content().string(Matchers.not(Matchers.containsString("senhaHash"))));

        mockMvc.perform(post("/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "vendedor1",
                                  "password": "StrongPass1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.login").value("vendedor1"))
                .andExpect(jsonPath("$.usuario.lojaId").value(lojaPadrao.getId()))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"))
                .andExpect(header().string("Set-Cookie", Matchers.allOf(
                        Matchers.containsString("isa_auth_token="),
                        Matchers.containsString("HttpOnly"),
                        Matchers.containsString("SameSite=Lax"))));
    }

    @Test
    void testProfileAllowsCorsFromAnyOrigin() throws Exception {
        mockMvc.perform(options("/signin")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void disabledUserCookieIsRejectedOnProtectedEndpoint() throws Exception {
        criarUsuario("admin", PerfilUsuario.ADMIN, lojaPadrao, true, "admin10");

        MvcResult loginResult = mockMvc.perform(post("/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "admin10"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String authCookieValue = extractCookieValue(loginResult.getResponse().getHeader("Set-Cookie"), "isa_auth_token");
        Usuario admin = usuarioRepository.findByLogin("admin").orElseThrow();
        admin.setAtivo(false);
        usuarioRepository.save(admin);

        mockMvc.perform(get("/api/usuarios/me").cookie(new Cookie("isa_auth_token", authCookieValue)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserEndpointReturnsStoreAndPerfilContext() throws Exception {
        criarUsuario("caixa-auth", PerfilUsuario.CAIXA, lojaPadrao, true, "StrongPass1");

        MvcResult loginResult = mockMvc.perform(post("/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "caixa-auth",
                                  "password": "StrongPass1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.login").value("caixa-auth"))
                .andExpect(jsonPath("$.usuario.perfil").value("CAIXA"))
                .andExpect(jsonPath("$.usuario.lojaId").value(lojaPadrao.getId()))
                .andReturn();

        String authCookieValue = extractCookieValue(loginResult.getResponse().getHeader("Set-Cookie"), "isa_auth_token");

        mockMvc.perform(get("/api/usuarios/me")
                        .cookie(new Cookie("isa_auth_token", authCookieValue)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("caixa-auth"))
                .andExpect(jsonPath("$.perfil").value("CAIXA"))
                .andExpect(jsonPath("$.lojaId").value(lojaPadrao.getId()))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
    }

    @Test
    void logoutClearsAuthenticationCookie() throws Exception {
        criarUsuario("logout-user", PerfilUsuario.CAIXA, lojaPadrao, true, "StrongPass1");

        mockMvc.perform(post("/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(header().string("Set-Cookie", Matchers.allOf(
                        Matchers.containsString("isa_auth_token="),
                        Matchers.containsString("Max-Age=0"),
                        Matchers.containsString("HttpOnly"),
                        Matchers.containsString("SameSite=Lax"))));
    }

    @Test
    void userCannotOpenSaleForAnotherDomainUser() throws Exception {
        Usuario caixa1 = criarUsuario("caixa1", PerfilUsuario.CAIXA, lojaPadrao, true);
        Usuario caixa2 = criarUsuario("caixa2", PerfilUsuario.CAIXA, lojaPadrao, true);

        mockMvc.perform(post("/api/pdv/vendas/abrir")
                        .with(user("caixa1").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "usuarioId": %d
                                }
                                """.formatted(caixa2.getId())))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/pdv/vendas/abrir")
                        .with(user("caixa1").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "usuarioId": %d
                                }
                                """.formatted(caixa1.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.id").value(caixa1.getId()))
                .andExpect(jsonPath("$.loja.id").value(lojaPadrao.getId()));
    }

    @Test
    void userCannotReadAnotherUsersSale() throws Exception {
        Usuario caixa1 = criarUsuario("caixa1", PerfilUsuario.CAIXA, lojaPadrao, true);
        criarUsuario("caixa2", PerfilUsuario.CAIXA, lojaPadrao, true);
        Venda venda = criarVenda(caixa1, lojaPadrao, StatusVenda.ABERTA);

        mockMvc.perform(get("/api/pdv/vendas/{vendaId}", venda.getId()).with(user("caixa2").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void negativeItemQuantityIsRejected() throws Exception {
        Usuario caixa = criarUsuario("caixa1", PerfilUsuario.CAIXA, lojaPadrao, true);
        Venda venda = criarVenda(caixa, lojaPadrao, StatusVenda.ABERTA);
        Produto produto = criarProduto("INT-001", "BAR-001", "Vestido");
        criarProdutoLoja(produto, lojaPadrao, 10);

        mockMvc.perform(post("/api/pdv/vendas/{vendaId}/itens", venda.getId())
                        .with(user("caixa1").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigoBarras": "BAR-001",
                                  "quantidade": -2
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void userCannotSearchProductsFromAnotherStore() throws Exception {
        Loja outraLoja = criarLoja("LOJA-2", "Loja 2");
        criarUsuario("caixa1", PerfilUsuario.CAIXA, lojaPadrao, true);

        mockMvc.perform(get("/api/produtos/buscar")
                        .with(user("caixa1").roles("USER"))
                        .param("lojaId", String.valueOf(outraLoja.getId()))
                        .param("nome", "cam"))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCanReadOwnStoreStockWithoutInformingLojaId() throws Exception {
        Loja outraLoja = criarLoja("LOJA-2", "Loja 2");
        criarUsuario("caixa1", PerfilUsuario.CAIXA, lojaPadrao, true);
        Produto produtoLoja1 = criarProduto("INT-010", "COD-010", "Calca");
        Produto produtoLoja2 = criarProduto("INT-011", "COD-011", "Saia");
        criarProdutoLoja(produtoLoja1, lojaPadrao, 4);
        criarProdutoLoja(produtoLoja2, outraLoja, 9);

        mockMvc.perform(get("/api/produtos/estoque")
                        .with(user("caixa1").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].loja.id").value(lojaPadrao.getId()))
                .andExpect(jsonPath("$", Matchers.hasSize(1)));
    }

    @Test
    void userCannotReadStockFromAnotherStore() throws Exception {
        Loja outraLoja = criarLoja("LOJA-2", "Loja 2");
        criarUsuario("caixa1", PerfilUsuario.CAIXA, lojaPadrao, true);

        mockMvc.perform(get("/api/produtos/estoque")
                        .with(user("caixa1").roles("USER"))
                        .param("lojaId", String.valueOf(outraLoja.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void userStockEntryUsesAuthenticatedUserAndOwnStore() throws Exception {
        Usuario caixa = criarUsuario("caixa1", PerfilUsuario.CAIXA, lojaPadrao, true);
        Produto produto = criarProduto("INT-020", "COD-020", "Blusa");

        mockMvc.perform(post("/api/estoque/movimentacoes/entrada")
                        .with(user("caixa1").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "produtoId": %d,
                                  "quantidade": 3,
                                  "observacao": "Reposicao"
                                }
                                """.formatted(produto.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loja.id").value(lojaPadrao.getId()))
                .andExpect(jsonPath("$.usuario.id").value(caixa.getId()))
                .andExpect(jsonPath("$.tipo").value("ENTRADA"))
                .andExpect(jsonPath("$.quantidade").value(3));

        ProdutoLoja estoqueAtual = produtoLojaRepository.findByProdutoIdAndLojaId(produto.getId(), lojaPadrao.getId())
                .orElseThrow();
        org.assertj.core.api.Assertions.assertThat(estoqueAtual.getQuantidadeEstoque()).isEqualTo(3);
    }

    @Test
    void userCannotRegisterStockEntryForAnotherStore() throws Exception {
        Loja outraLoja = criarLoja("LOJA-2", "Loja 2");
        criarUsuario("caixa1", PerfilUsuario.CAIXA, lojaPadrao, true);
        Produto produto = criarProduto("INT-021", "COD-021", "Jaqueta");

        mockMvc.perform(post("/api/estoque/movimentacoes/entrada")
                        .with(user("caixa1").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "produtoId": %d,
                                  "lojaId": %d,
                                  "quantidade": 2
                                }
                                """.formatted(produto.getId(), outraLoja.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanRegisterStockEntryForChosenStore() throws Exception {
        Loja outraLoja = criarLoja("LOJA-2", "Loja 2");
        Usuario admin = criarUsuario("admin", PerfilUsuario.ADMIN, lojaPadrao, true);
        Produto produto = criarProduto("INT-022", "COD-022", "Casaco");

        mockMvc.perform(post("/api/estoque/movimentacoes/entrada")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "produtoId": %d,
                                  "lojaId": %d,
                                  "quantidade": 5
                                }
                                """.formatted(produto.getId(), outraLoja.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loja.id").value(outraLoja.getId()))
                .andExpect(jsonPath("$.usuario.id").value(admin.getId()))
                .andExpect(jsonPath("$.tipo").value("ENTRADA"));
    }

    @Test
    void productSearchForUserDoesNotExposeSensitiveFields() throws Exception {
        criarUsuario("caixa1", PerfilUsuario.CAIXA, lojaPadrao, true);
        Produto produto = criarProduto("INT-002", "COD-002", "Camiseta");
        criarProdutoLoja(produto, lojaPadrao, 7);

        mockMvc.perform(get("/api/produtos/buscar")
                        .with(user("caixa1").roles("USER"))
                        .param("lojaId", String.valueOf(lojaPadrao.getId()))
                        .param("nome", "cam"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].descricao").value("Camiseta"))
                .andExpect(content().string(Matchers.not(Matchers.containsString("precoCusto"))))
                .andExpect(content().string(Matchers.not(Matchers.containsString("codigoInterno"))))
                .andExpect(content().string(Matchers.not(Matchers.containsString("cnpj"))));
    }

    private Loja criarLoja(String codigo, String nome) {
        Loja loja = new Loja();
        loja.setCodigo(codigo);
        loja.setNome(nome);
        loja.setAtivo(true);
        return lojaRepository.save(loja);
    }

    private Usuario criarUsuario(String login, PerfilUsuario perfil, Loja loja, boolean ativo) {
        return criarUsuario(login, perfil, loja, ativo, "StrongPass1");
    }

    private Usuario criarUsuario(String login, PerfilUsuario perfil, Loja loja, boolean ativo, String senha) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setNome("Usuario " + login);
        usuario.setPerfil(perfil);
        usuario.setAtivo(ativo);
        usuario.setSenhaHash(passwordEncoder.encode(senha));
        usuario.setLoja(loja);
        return usuarioRepository.save(usuario);
    }

    private Venda criarVenda(Usuario usuario, Loja loja, StatusVenda status) {
        Venda venda = new Venda();
        venda.setUsuario(usuario);
        venda.setLoja(loja);
        venda.setStatus(status);
        return vendaRepository.save(venda);
    }

    private Produto criarProduto(String codigoInterno, String codigoBarras, String descricao) {
        Produto produto = new Produto();
        produto.setCodigoInterno(codigoInterno);
        produto.setCodigoBarras(codigoBarras);
        produto.setDescricao(descricao);
        produto.setPrecoCusto(BigDecimal.valueOf(10));
        produto.setPrecoVenda(BigDecimal.valueOf(20));
        produto.setAtivo(true);
        return produtoRepository.save(produto);
    }

    private ProdutoLoja criarProdutoLoja(Produto produto, Loja loja, int quantidadeEstoque) {
        ProdutoLoja produtoLoja = new ProdutoLoja(produto, loja, quantidadeEstoque);
        return produtoLojaRepository.save(produtoLoja);
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
