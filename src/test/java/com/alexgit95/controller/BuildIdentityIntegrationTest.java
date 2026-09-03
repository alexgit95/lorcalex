package com.alexgit95.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class BuildIdentityIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private BuildProperties buildProperties;

    @Test
    @WithMockUser
    @DisplayName("admin can retrieve the active build version and short commit")
    void getBuildIdentity_returnsActiveBuildMetadata() throws Exception {
        String commit = buildProperties.get("git.commit");
        assertThat(commit).isNotBlank();
        String shortCommit = commit.length() > 7 ? commit.substring(0, 7) : commit;

        mockMvc.perform(get("/api/admin/version"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(buildProperties.getVersion()))
                .andExpect(jsonPath("$.commit").value(shortCommit));
    }

    @Test
    @DisplayName("build identity requires authentication")
    void getBuildIdentity_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/version"))
                .andExpect(status().isForbidden());
    }
}