package com.example.heritage_sharing_api.integration.page;

import com.example.heritage_sharing_api.integration.support.IntegrationTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@DisplayName("Integration tests for basic page endpoints")
public class BasicPageEndpointIntegrationTests extends IntegrationTestSupport {

    @Test
    @DisplayName("IT-PAGE-01: health endpoint returns ok json")
    public void healthEndpointReturnsOkJson() throws Exception {
        var result = mvc.perform(get("/api/health")
                .with(authentication(authenticationFor(regularUser)))).andReturn();

        Assertions.assertEquals(200, result.getResponse().getStatus());
        Assertions.assertEquals("{\"status\":\"ok\"}", result.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("IT-PAGE-02: favicon endpoint redirects to static favicon")
    public void faviconEndpointRedirectsToStaticFavicon() throws Exception {
        var result = mvc.perform(get("/favicon.ico")).andReturn();

        Assertions.assertEquals(302, result.getResponse().getStatus());
        Assertions.assertEquals("/static/favicon.svg", result.getResponse().getRedirectedUrl());
    }
}
