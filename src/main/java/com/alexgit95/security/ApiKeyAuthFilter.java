package com.alexgit95.security;

import com.alexgit95.service.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtre optionnel utilisable si l'on souhaitait brancher la validation de clé API
 * directement dans la chaîne Spring Security.
 * <p>
 * Note : la solution retenue est plus simple — la validation est effectuée directement
 * dans {@code ExportController}, et {@code /api/export} est déclaré {@code .permitAll()}
 * dans {@code SecurityConfig}.
 */
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String EXPORT_PATH = "/api/export";

    private final ApiKeyService apiKeyService;

    public ApiKeyAuthFilter(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !EXPORT_PATH.equals(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String rawKey = request.getParameter("apiKey");

        apiKeyService.validateAndTouch(rawKey).ifPresent(apiKey -> {
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    "api:" + apiKey.getName(),
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_API"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        });

        filterChain.doFilter(request, response);
    }
}
