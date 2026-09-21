package leonardo.banking_transactions.middlewares;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import leonardo.banking_transactions.exceptions.JwtInvalidOrMissingException;
import leonardo.banking_transactions.exceptions.UserNotAllowedException;
import leonardo.banking_transactions.services.JwtService;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class JwtAuthenticationMiddleware extends OncePerRequestFilter {

    private static final String USERS_PATH = "/api/users/";
    private final JwtService jwtService;

    public JwtAuthenticationMiddleware(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String method = request.getMethod();

        boolean protectedMethod = HttpMethod.GET.matches(method)
                || HttpMethod.PUT.matches(method)
                || HttpMethod.DELETE.matches(method);

        return !protectedMethod || !path.startsWith(USERS_PATH)
                || path.substring(USERS_PATH.length()).contains("/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");

        if (authorization == null || !authorization.startsWith("Bearer ")
                || authorization.substring(7).isBlank()) {
            throw new UserNotAllowedException();
        }

        try {
            UUID tokenUserId = jwtService.extractUserId(authorization.substring(7).trim());
            UUID requestedUserId = UUID.fromString(
                    request.getRequestURI()
                            .substring(request.getContextPath().length() + USERS_PATH.length())
            );

            if (!tokenUserId.equals(requestedUserId)) {
                throw new UserNotAllowedException();
            }

            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new JwtInvalidOrMissingException();
        }
    }
}
