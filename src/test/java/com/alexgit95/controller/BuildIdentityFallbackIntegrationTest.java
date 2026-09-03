package com.alexgit95.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.profiles.active=test",
        "spring.info.build.location=classpath:META-INF/missing-build-info.properties"
})
@AutoConfigureMockMvc
class BuildIdentityFallbackIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Test
    @WithMockUser
    @DisplayName("build identity falls back when packaged metadata is unavailable")
    void getBuildIdentity_usesFallbackWithoutBuildMetadata() throws Exception {
        mockMvc.perform(get("/api/admin/version"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("unknown"))
                .andExpect(jsonPath("$.commit").value("unknown"));
    }
}