package com.alexgit95.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JPA slice tests for UserCollection auditing fields:
 * foil, firstAddedAt, lastAddedAt.
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.profiles.active=test",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class UserCollectionAuditTest {

    @Autowired
    private TestEntityManager em;

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Card persistCard() {
        Card card = new Card();
        card.setName("Elsa - Snow Queen");
        return em.persistAndFlush(card);
    }

    // ─── @PrePersist ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("@PrePersist sets firstAddedAt and lastAddedAt when they are null")
    void onCreate_setsFirstAddedAtAndLastAddedAt() {
        Card card = persistCard();

        UserCollection uc = new UserCollection();
        uc.setCard(card);
        uc.setQuantity(1);

        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        UserCollection saved = em.persistFlushFind(uc);
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertThat(saved.getFirstAddedAt()).isAfter(before).isBefore(after);
        assertThat(saved.getLastAddedAt()).isAfter(before).isBefore(after);
    }

    @Test
    @DisplayName("@PrePersist does NOT overwrite firstAddedAt when it is preset (restore scenario)")
    void onCreate_doesNotOverwritePresetFirstAddedAt() {
        Card card = persistCard();
        LocalDateTime originalDate = LocalDateTime.of(2025, 3, 10, 8, 0, 0);

        UserCollection uc = new UserCollection();
        uc.setCard(card);
        uc.setQuantity(2);
        uc.setFirstAddedAt(originalDate);
        uc.setLastAddedAt(originalDate.plusDays(1));

        UserCollection saved = em.persistFlushFind(uc);

        assertThat(saved.getFirstAddedAt()).isEqualTo(originalDate);
        assertThat(saved.getLastAddedAt()).isEqualTo(originalDate.plusDays(1));
    }

    // ─── @PreUpdate ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("@PreUpdate updates lastAddedAt but preserves firstAddedAt")
    void onUpdate_updatesLastAddedAtAndPreservesFirstAddedAt() throws InterruptedException {
        Card card = persistCard();

        UserCollection uc = new UserCollection();
        uc.setCard(card);
        uc.setQuantity(1);
        UserCollection saved = em.persistFlushFind(uc);
        LocalDateTime firstAdded = saved.getFirstAddedAt();

        // Small delay so timestamps can differ on fast machines
        Thread.sleep(50);

        saved.setQuantity(3);
        em.flush();

        assertThat(saved.getFirstAddedAt()).isEqualTo(firstAdded);
        assertThat(saved.getLastAddedAt()).isNotNull().isAfterOrEqualTo(firstAdded);
    }

    // ─── foil ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("foil defaults to false when not set")
    void foil_defaultsToFalse() {
        Card card = persistCard();

        UserCollection uc = new UserCollection();
        uc.setCard(card);
        uc.setQuantity(1);
        UserCollection saved = em.persistFlushFind(uc);

        assertThat(saved.getFoil()).isFalse();
    }

    @Test
    @DisplayName("foil is persisted as true when set")
    void foil_persistedTrue() {
        Card card = persistCard();

        UserCollection uc = new UserCollection();
        uc.setCard(card);
        uc.setQuantity(1);
        uc.setFoil(true);
        UserCollection saved = em.persistFlushFind(uc);

        assertThat(saved.getFoil()).isTrue();
    }

    @Test
    @DisplayName("foil can be toggled from false to true via update")
    void foil_canBeToggledViaUpdate() {
        Card card = persistCard();

        UserCollection uc = new UserCollection();
        uc.setCard(card);
        uc.setQuantity(1);
        uc.setFoil(false);
        UserCollection saved = em.persistFlushFind(uc);

        saved.setFoil(true);
        em.flush();

        assertThat(saved.getFoil()).isTrue();
    }
}
