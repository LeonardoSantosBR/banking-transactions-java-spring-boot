package leonardo.banking_transactions.middlewares;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import leonardo.banking_transactions.config.JwtAccessDeniedHandler;
import leonardo.banking_transactions.config.JwtAuthenticationEntryPoint;
import leonardo.banking_transactions.exceptions.JwtInvalidOrMissingException;
import leonardo.banking_transactions.exceptions.UserNotAllowedException;
import leonardo.banking_transactions.repositories.UserTokenVersionsRepository;
import leonardo.banking_transactions.repositories.UsersRepository;
import leonardo.banking_transactions.services.JwtService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Component
public class JwtAuthenticationMiddleware extends OncePerRequestFilter {
    public static final String AUTHENTICATED_USER_ID_ATTRIBUTE = "authenticatedUserId";
    private static final Set<String> PUBLIC_ENDPOINTS = Set.of("POST /api/users", "POST /api/auth/login");
    private static final String USERS_PATH = "/api/users/";
    private static final String ACCOUNTS_PATH = "/api/accounts/";
    private final JwtService jwtService;
    private final UsersRepository usersRepository;
    private final UserTokenVersionsRepository tokenVersionsRepository;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;

    public JwtAuthenticationMiddleware(
            JwtService jwtService,
            UsersRepository usersRepository,
            UserTokenVersionsRepository tokenVersionsRepository,
            JwtAuthenticationEntryPoint authenticationEntryPoint,
            JwtAccessDeniedHandler accessDeniedHandler) {
        this.jwtService = jwtService;
        this.usersRepository = usersRepository;
        this.tokenVersionsRepository = tokenVersionsRepository;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return PUBLIC_ENDPOINTS.contains(request.getMethod() + " " + path);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.substring(7).isBlank()) {
            rejectAuthentication(request, response, null);
            return;
        }

        UUID tokenUserId;
        int tokenVersion;
        
        try {
            String token = authorization.substring(7).trim();
            tokenUserId = jwtService.extractUserId(token);
            tokenVersion = jwtService.extractTokenVersion(token);
        } catch (JwtException | IllegalArgumentException exception) {
            rejectAuthentication(request, response, exception);
            return;
        }

        if (usersRepository.findByIdAndDeletedAtIsNull(tokenUserId).isEmpty()
                || !tokenVersionsRepository.existsByUserIdAndTokenVersion(tokenUserId, tokenVersion)) {
            rejectAuthentication(request, response, null);
            return;
        }

        request.setAttribute(AUTHENTICATED_USER_ID_ATTRIBUTE, tokenUserId);
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String resourcePath = path.startsWith(USERS_PATH) ? USERS_PATH
                : path.startsWith(ACCOUNTS_PATH) ? ACCOUNTS_PATH : null;

        if (resourcePath != null) {
            String userId = path.substring(resourcePath.length()).split("/")[0];
            UUID requestedUserId;
            try {
                requestedUserId = UUID.fromString(userId);
            } catch (IllegalArgumentException exception) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid user ID");
                return;
            }
            if (!tokenUserId.equals(requestedUserId)) {
                rejectAuthorization(request, response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void rejectAuthentication(HttpServletRequest request, HttpServletResponse response, Exception cause)
            throws IOException {
        String message = new JwtInvalidOrMissingException().getMessage();
        authenticationEntryPoint.commence(request, response, new BadCredentialsException(message, cause));
    }

    private void rejectAuthorization(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        String message = new UserNotAllowedException().getMessage();
        accessDeniedHandler.handle(request, response, new AccessDeniedException(message));
    }
}