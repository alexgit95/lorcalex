package com.alexgit95.controller;

import com.alexgit95.service.ApiKeyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Endpoints de gestion des clés API (CRUD).
 * Tous les endpoints requièrent une authentification JWT (admin).
 */
@RestController
@RequestMapping("/api/admin/apikeys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    /** Liste toutes les clés (sans la valeur en clair ni le hash). */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list() {
        return ResponseEntity.ok(apiKeyService.listKeys());
    }

    /**
     * Crée une nouvelle clé API.
     * Corps attendu : {@code { "name": "...", "validityDays": 30 }}.
     * Retourne la clé en clair <em>une seule fois</em> dans le champ {@code "key"}.
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Le nom est obligatoire."));
        }
        int validityDays = body.containsKey("validityDays")
                ? ((Number) body.get("validityDays")).intValue()
                : 30;
        if (validityDays < 1 || validityDays > 3650) {
            return ResponseEntity.badRequest().body(Map.of("error", "Durée invalide (1–3650 jours)."));
        }
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(validityDays);
        return ResponseEntity.ok(apiKeyService.generateKey(name, expiresAt));
    }

    /** Supprime une clé par son identifiant. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        if (apiKeyService.deleteKey(id)) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Clé supprimée."));
        }
        return ResponseEntity.status(404).body(Map.of("success", false, "message", "Clé introuvable."));
    }
}
