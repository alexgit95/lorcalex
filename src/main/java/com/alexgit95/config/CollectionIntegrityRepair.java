package com.alexgit95.config;

import com.alexgit95.model.UserCollection;
import com.alexgit95.repository.UserCollectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
public class CollectionIntegrityRepair {

    private static final Logger log = LoggerFactory.getLogger(CollectionIntegrityRepair.class);

    private final UserCollectionRepository userCollectionRepository;

    public CollectionIntegrityRepair(UserCollectionRepository userCollectionRepository) {
        this.userCollectionRepository = userCollectionRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void repairFoilInvariantIfNeeded() {
        List<UserCollection> all = userCollectionRepository.findAll();
        List<UserCollection> dirty = new ArrayList<>();
        int repaired = 0;

        for (UserCollection uc : all) {
            int qty = uc.getQuantity() != null ? uc.getQuantity() : 0;
            int foilQty = uc.getFoilQuantity() != null ? uc.getFoilQuantity() : 0;
            boolean expectedFoil = foilQty > 0;

            boolean changed = false;
            if (uc.getQuantity() == null) {
                uc.setQuantity(0);
                changed = true;
            }
            if (uc.getFoilQuantity() == null) {
                uc.setFoilQuantity(0);
                changed = true;
            }
            if (uc.getFoil() == null || uc.getFoil() != expectedFoil) {
                uc.setFoil(expectedFoil);
                changed = true;
            }

            // Keep data shape clean: no row should remain owned=false.
            if (qty <= 0 && foilQty <= 0) {
                userCollectionRepository.delete(uc);
                repaired++;
                continue;
            }

            if (changed) {
                dirty.add(uc);
                repaired++;
            }
        }

        if (!dirty.isEmpty()) {
            userCollectionRepository.saveAll(dirty);
        }

        if (repaired > 0) {
            log.info("Collection integrity repair updated {} row(s).", repaired);
        }
    }
}
