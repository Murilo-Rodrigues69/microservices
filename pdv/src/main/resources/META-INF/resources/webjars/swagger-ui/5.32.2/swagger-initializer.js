window.onload = function() {
  function extractLoginPayload(response) {
    if (!response) {
      return null;
    }

    if (response.body && typeof response.body === "object") {
      return response.body;
    }

    if (typeof response.text === "string" && response.text.length > 0) {
      try {
        return JSON.parse(response.text);
      } catch (error) {
        console.warn("Nao foi possivel interpretar a resposta do /signin no Swagger.", error);
      }
    }

    return null;
  }

  function autoAuthorizeFromLogin(response) {
    const isSigninResponse = response
      && typeof response.url === "string"
      && response.url.endsWith("/signin")
      && response.status === 200;

    if (!isSigninResponse || !window.ui) {
      return response;
    }

    const payload = extractLoginPayload(response);
    if (payload && payload.jwtToken) {
      window.ui.preauthorizeApiKey("bearerAuth", payload.jwtToken);
    }

    return response;
  }

  //<editor-fold desc="Changeable Configuration Block">
  window.ui = SwaggerUIBundle({
    url: "",
    dom_id: "#swagger-ui",
    deepLinking: true,
    presets: [
      SwaggerUIBundle.presets.apis,
      SwaggerUIStandalonePreset
    ],
    plugins: [
      SwaggerUIBundle.plugins.DownloadUrl
    ],
    layout: "StandaloneLayout",
    configUrl: "/v3/api-docs/swagger-config",
    persistAuthorization: true,
    validatorUrl: "",
    responseInterceptor: autoAuthorizeFromLogin
  });
  //</editor-fold>
};
