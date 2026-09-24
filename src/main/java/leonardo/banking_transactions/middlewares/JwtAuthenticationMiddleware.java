package leonardo.banking_transactions.middlewares;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import leonardo.banking_transactions.config.JwtAuthenticationEntryPoint;
import leonardo.banking_transactions.exceptions.JwtInvalidOrMissingException;
import leonardo.banking_transactions.exceptions.UserNotAllowedException;
import leonardo.banking_transactions.services.JwtService;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.authentication.BadCredentialsException;

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
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;

    public JwtAuthenticationMiddleware(
            JwtService jwtService,
            JwtAuthenticationEntryPoint authenticationEntryPoint) {

        this.jwtService = jwtService;
        this.authenticationEntryPoint = authenticationEntryPoint;
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
        try {
            String authorization = request.getHeader("Authorization");

            if (authorization == null || !authorization.startsWith("Bearer ")
                    || authorization.substring(7).isBlank()) {
                throw new UserNotAllowedException();
            }

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
            handleAuthenticationFailure(
                    request,
                    response,
                    new JwtInvalidOrMissingException().getMessage(),
                    exception);
        } catch (UserNotAllowedException exception) {
            handleAuthenticationFailure(
                    request,
                    response,
                    exception.getMessage(),
                    exception);
        }
    }

    private void handleAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            String message,
            Exception cause) throws IOException {

        authenticationEntryPoint.commence(
                request,
                response,
                new BadCredentialsException(message, cause));
    }
}
