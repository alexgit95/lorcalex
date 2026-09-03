package com.alexgit95.controller;

import com.alexgit95.model.Card;
import com.alexgit95.model.Edition;
import com.alexgit95.model.UserCollection;
import com.alexgit95.repository.CardRepository;
import com.alexgit95.repository.EditionRepository;
import com.alexgit95.repository.UserCollectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class DreambornExportIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private EditionRepository editionRepository;
    @Autowired private CardRepository cardRepository;
    @Autowired private UserCollectionRepository collectionRepository;

    @BeforeEach
    void clean() {
        collectionRepository.deleteAllInBatch();
        cardRepository.deleteAllInBatch();
        editionRepository.deleteAllInBatch();
    }

    @Test
    @WithMockUser
    @DisplayName("Dreamborn export reserves foil by default without changing the collection")
    void export_reservesFoilByDefault() throws Exception {
        Edition edition = saveEdition("URS", 4);
        Card card = saveCard("Elsa", 188, edition);
        UserCollection collection = saveCollection(card, 2, 1);

        MvcResult result = mockMvc.perform(get("/api/admin/export/dreamborn"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentType()).startsWith("text/csv");
        assertThat(result.getResponse().getHeader("Content-Disposition"))
                .isEqualTo("attachment; filename=\"lorcalex-dreamborn.csv\"");
        assertThat(result.getResponse().getContentAsString())
                .isEqualTo("Set Number,Card Number,Variant,Count\n4,188,normal,2\n");
        UserCollection persisted = collectionRepository.findById(collection.getId()).orElseThrow();
        assertThat(persisted.getQuantity()).isEqualTo(2);
        assertThat(persisted.getFoilQuantity()).isEqualTo(1);
    }

    @Test
    @WithMockUser
    @DisplayName("Dreamborn export keeps every quantity when reserve is disabled")
    void export_withoutReserve_keepsAllQuantities() throws Exception {
        Edition edition = saveEdition("URS", 4);
        Card card = saveCard("Elsa", 188, edition);
        saveCollection(card, 2, 1);

        String csv = mockMvc.perform(get("/api/admin/export/dreamborn").param("reserve", "false"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(csv).isEqualTo("Set Number,Card Number,Variant,Count\n"
                + "4,188,normal,2\n4,188,foil,1\n");
    }

    @Test
    @WithMockUser
    @DisplayName("Dreamborn export omits reserved singles and cards without identifiers")
    void export_omitsSinglesAndMissingIdentifiers() throws Exception {
        Edition validEdition = saveEdition("TFC", 1);
        saveCollection(saveCard("Playable", 13, validEdition), 2, 0);
        saveCollection(saveCard("Single foil", 14, validEdition), 0, 1);
        saveCollection(saveCard("Missing card number", null, validEdition), 3, 0);

        Edition missingSetEdition = saveEdition("UNKNOWN", null);
        saveCollection(saveCard("Missing set number", 15, missingSetEdition), 3, 0);

        String csv = mockMvc.perform(get("/api/admin/export/dreamborn"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(csv).isEqualTo("Set Number,Card Number,Variant,Count\n1,13,normal,1\n");
    }

    @Test
    @DisplayName("Dreamborn export requires authentication")
    void export_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/export/dreamborn"))
                .andExpect(status().isForbidden());
    }

    private Edition saveEdition(String code, Integer setNumber) {
        Edition edition = new Edition();
        edition.setCode(code);
        edition.setName(code);
        edition.setSetNumber(setNumber);
        return editionRepository.save(edition);
    }

    private Card saveCard(String name, Integer cardNumber, Edition edition) {
        Card card = new Card();
        card.setName(name);
        card.setCardNumber(cardNumber);
        card.setEdition(edition);
        return cardRepository.save(card);
    }

    private UserCollection saveCollection(Card card, int normalQuantity, int foilQuantity) {
        UserCollection collection = new UserCollection();
        collection.setCard(card);
        collection.setQuantity(normalQuantity);
        collection.setFoilQuantity(foilQuantity);
        collection.setFoil(foilQuantity > 0);
        return collectionRepository.save(collection);
    }
}