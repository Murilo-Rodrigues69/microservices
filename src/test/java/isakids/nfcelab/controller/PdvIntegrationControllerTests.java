package isakids.nfcelab.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import isakids.nfcelab.config.PdvIntegrationProperties;
import isakids.nfcelab.dto.PdvNfceIntegrationRequest;
import isakids.nfcelab.service.PdvIntegrationService;

class PdvIntegrationControllerTests {
    private final PdvIntegrationService service = mock(PdvIntegrationService.class);
    private final PdvIntegrationProperties properties = new PdvIntegrationProperties();
    private final PdvIntegrationController controller = new PdvIntegrationController(service, properties);

    @Test
    void missingConfigurationDisablesEmissionAndDownloads() {
        assertThatThrownBy(() -> controller.emitirNfce(null, new PdvNfceIntegrationRequest()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode().value()).isEqualTo(503));
        assertThatThrownBy(() -> controller.baixarPdf(null, 1L, "11222333000144"))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> controller.baixarXml(null, 1L, "11222333000144"))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(service);
    }

    @Test
    void rejectsMissingOrInvalidTokenBeforeCallingService() {
        properties.setToken("token-exclusivo-de-teste");
        for (String token : new String[] { null, "incorreto" }) {
            assertThatThrownBy(() -> controller.emitirNfce(token, new PdvNfceIntegrationRequest()))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            ex -> assertThat(ex.getStatusCode().value()).isEqualTo(401));
        }
        verifyNoInteractions(service);
    }

    @Test
    void acceptsConfiguredToken() {
        properties.setToken("token-exclusivo-de-teste");
        PdvNfceIntegrationRequest request = new PdvNfceIntegrationRequest();
        controller.emitirNfce("token-exclusivo-de-teste", request);
        verify(service).emitirNfce(request);
    }
}
