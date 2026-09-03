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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class CardListingPayloadIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private CardRepository cardRepository;
    @Autowired private EditionRepository editionRepository;
    @Autowired private UserCollectionRepository collectionRepository;

    private Card card;

    @BeforeEach
    void setup() {
        collectionRepository.deleteAllInBatch();
        cardRepository.deleteAllInBatch();
        editionRepository.deleteAllInBatch();

        Edition edition = new Edition();
        edition.setCode("TFC");
        edition.setName("Premier Chapitre");
        edition = editionRepository.save(edition);

        card = new Card();
        card.setEdition(edition);
        card.setName("Elsa");
        card.setCardNumber(1);
        card.setBodyText("Corps de texte des règles");
        card.setFlavorText("Texte d'ambiance");
        card.setThumbnailUrl("https://example.com/thumb.jpg");
        card = cardRepository.save(card);
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/cards omits rules text fields")
    void listCards_omitsRulesText() throws Exception {
        mockMvc.perform(get("/api/cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bodyText").doesNotExist())
                .andExpect(jsonPath("$[0].flavorText").doesNotExist())
                .andExpect(jsonPath("$[0].thumbnailUrl").value("https://example.com/thumb.jpg"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/cards/{id} keeps rules text fields")
    void getCard_keepsRulesText() throws Exception {
        mockMvc.perform(get("/api/cards/{id}", card.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bodyText").value("Corps de texte des règles"))
                .andExpect(jsonPath("$.flavorText").value("Texte d'ambiance"));
    }
}
