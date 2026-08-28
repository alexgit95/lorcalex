package com.alexgit95.controller;

import com.alexgit95.model.Card;
import com.alexgit95.model.Edition;
import com.alexgit95.repository.CardRepository;
import com.alexgit95.repository.EditionRepository;
import com.alexgit95.repository.UserCollectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class CardWantedIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private CardRepository cardRepository;
    @Autowired private EditionRepository editionRepository;
    @Autowired private UserCollectionRepository collectionRepository;

    @BeforeEach
    void clean() {
        collectionRepository.deleteAllInBatch();
        cardRepository.deleteAllInBatch();
        editionRepository.deleteAllInBatch();
    }

    @Test
    @WithMockUser
    @DisplayName("PATCH /api/cards/{id}/wanted marks a card wanted")
    void setWanted_marksCard() throws Exception {
        Edition edition = new Edition();
        edition.setCode("TFC");
        edition.setName("Premier Chapitre");
        edition = editionRepository.save(edition);

        Card card = new Card();
        card.setEdition(edition);
        card.setName("Elsa");
        card.setCardNumber(1);
        card = cardRepository.save(card);

        mockMvc.perform(patch("/api/cards/{id}/wanted", card.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"wanted\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wanted").value(true))
                .andExpect(jsonPath("$.owned").value(false));
    }

    @Test
    @WithMockUser
    @DisplayName("PATCH /api/cards/{id}/wanted returns 404 for unknown card")
    void setWanted_unknownCard_returnsNotFound() throws Exception {
        mockMvc.perform(patch("/api/cards/{id}/wanted", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"wanted\": true}"))
                .andExpect(status().isNotFound());
    }
}
