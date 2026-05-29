package com.example.heritage_sharing_api.unit;

import com.example.heritage_sharing_api.controller.TaxonomyController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Unit tests for TaxonomyController")
class TaxonomyControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TaxonomyController()).build();
    }

    @Test
    @DisplayName("UT-TAX-01: admin route redirects to taxonomy page")
    void adminRouteRedirectsToTaxonomyPage() throws Exception {
        mockMvc.perform(get("/api/taxonomy/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/html/admin/taxonomy.html"));
    }

    @Test
    @DisplayName("UT-TAX-02: root route redirects to taxonomy page")
    void rootRouteRedirectsToTaxonomyPage() throws Exception {
        mockMvc.perform(get("/api/taxonomy/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/html/admin/taxonomy.html"));
    }
}
