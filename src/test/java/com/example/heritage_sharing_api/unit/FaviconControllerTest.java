package com.example.heritage_sharing_api.unit;

import com.example.heritage_sharing_api.controller.FaviconController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Unit tests for FaviconController")
class FaviconControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FaviconController()).build();
    }

    @Test
    @DisplayName("UT-FAV-01: favicon route redirects to svg asset")
    void faviconRouteRedirectsToSvgAsset() throws Exception {
        mockMvc.perform(get("/favicon.ico"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/static/favicon.svg"));
    }
}
