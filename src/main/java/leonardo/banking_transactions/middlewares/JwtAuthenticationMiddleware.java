package leonardo.banking_transactions.middlewares;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import leonardo.banking_transactions.exceptions.JwtInvalidOrMissingException;
import leonardo.banking_transactions.exceptions.UserNotAllowedException;
import leonardo.banking_transactions.services.JwtService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Component
public class JwtAuthenticationMiddleware extends OncePerRequestFilter {

    private static final Set<String> PUBLIC_ENDPOINTS = Set.of(
            "POST /api/users",
            "GET /api/users",
            "POST /api/auth/login");
    private static final String USERS_PATH = "/api/users/";
    private static final String ACCOUNTS_PATH = "/api/accounts/";
    private final JwtService jwtService;

    public JwtAuthenticationMiddleware(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI()
                .substring(request.getContextPath().length());
        String endpoint = request.getMethod() + " " + path;

        return PUBLIC_ENDPOINTS.contains(endpoint);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");

        if (authorization == null || !authorization.startsWith("Bearer ")
                || authorization.substring(7).isBlank()) {
            throw new UserNotAllowedException();
        }

        try {
            UUID tokenUserId = jwtService.extractUserId(authorization.substring(7).trim());
            String path = request.getRequestURI().substring(request.getContextPath().length());
            String resourcePath = path.startsWith(USERS_PATH) ? USERS_PATH
                    : path.startsWith(ACCOUNTS_PATH) ? ACCOUNTS_PATH : null;

            if (resourcePath != null) {
                String userId = path.substring(resourcePath.length()).split("/")[0];
                UUID requestedUserId = UUID.fromString(userId);

                if (!tokenUserId.equals(requestedUserId)) {
                    throw new UserNotAllowedException();
                }
            }

            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new JwtInvalidOrMissingException();
        }
    }
}
