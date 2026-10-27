package isakids.isa;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class SwaggerOpenApiTests {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void apiDocsEndpointLoads() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    void swaggerUiPageLoads() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void swaggerUiConfigKeepsAuthorizationPersisted() throws Exception {
        mockMvc.perform(get("/v3/api-docs/swagger-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.persistAuthorization").value(true));
    }

    @Test
    void swaggerInitializerEnablesAutomaticJwtAuthorizationAfterSignin() throws Exception {
        mockMvc.perform(get("/swagger-ui/swagger-initializer.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("preauthorizeApiKey(\"bearerAuth\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("responseInterceptor: autoAuthorizeFromLogin")));
    }
}
