package leonardo.banking_transactions.middlewares;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import leonardo.banking_transactions.entities.SecurityAuditEventType;
import leonardo.banking_transactions.exceptions.RateLimitExceededException;
import leonardo.banking_transactions.services.RateLimitService;
import leonardo.banking_transactions.services.SecurityAuditService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String USER_CREATE_PATH = "/api/users";
    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);
    private final RateLimitService rateLimitService;
    private final SecurityAuditService securityAuditService;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(
            RateLimitService rateLimitService,
            SecurityAuditService securityAuditService,
            ObjectMapper objectMapper) {
        this.rateLimitService = rateLimitService;
        this.securityAuditService = securityAuditService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod()))
            return true;
        String path = request.getServletPath();
        return !LOGIN_PATH.equals(path)
                && !USER_CREATE_PATH.equals(path)
                && !(USER_CREATE_PATH + "/").equals(path);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String cpf = "";
        byte[] body = request.getInputStream().readNBytes(8193);
        if (body.length > 8192) {
            if (!recordEvent(request, cpf, eventType(request.getServletPath(), 413))) {
                writeError(response, 503, "Request processing is temporarily unavailable.");
                return;
            }
            writeError(response, 413, "Request body is too large.");
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(body);
            if (node != null && node.hasNonNull("cpf"))
                cpf = node.get("cpf").asText("");
        } catch (JsonProcessingException ignored) {
        }
        try {
            if (LOGIN_PATH.equals(request.getServletPath()))
                rateLimitService.checkAndConsumeLogin(request.getRemoteAddr(), cpf);
            else
                rateLimitService.checkAndConsumeUserCreation(request.getRemoteAddr(), cpf);
        } catch (RateLimitExceededException exception) {
            SecurityAuditEventType eventType = eventType(request.getServletPath(), 429);
            if (!recordEvent(request, cpf, eventType)) {
                writeError(response, 503, "Request processing is temporarily unavailable.");
                return;
            }
            writeError(response, 429, "Too many requests. Try again later.");
            return;
        } catch (RuntimeException exception) {
            SecurityAuditEventType eventType = eventType(request.getServletPath(), 503);
            recordEvent(request, cpf, eventType);
            writeError(response, 503, "Request processing is temporarily unavailable.");
            return;
        }
        ContentCachingResponseWrapper bufferedResponse = new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(new CachedBodyRequest(request, body), bufferedResponse);
        } catch (IOException | ServletException | RuntimeException exception) {
            recordEvent(request, cpf, eventType(request.getServletPath(), 503));
            throw exception;
        }
        SecurityAuditEventType responseEventType = eventType(request.getServletPath(), bufferedResponse.getStatus());
        try {
            securityAuditService.record(
                    responseEventType,
                    cpf,
                    request.getRemoteAddr(),
                    request.getHeader("User-Agent"));
        } catch (RuntimeException exception) {
            logger.error("Could not persist security audit event for a public endpoint");
            if (responseEventType != SecurityAuditEventType.USER_REGISTRATION_SUCCEEDED) {
                response.reset();
                writeError(response, 503, "Request processing is temporarily unavailable.");
                return;
            }
        }
        bufferedResponse.copyBodyToResponse();
    }

    private boolean recordEvent(
            HttpServletRequest request,
            String cpf,
            SecurityAuditEventType eventType) {
        try {
            securityAuditService.record(
                    eventType,
                    cpf,
                    request.getRemoteAddr(),
                    request.getHeader("User-Agent"));
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private SecurityAuditEventType eventType(String path, int status) {
        AuditedOperation operation = AuditedOperation.fromPath(path);
        if (operation == null)
            return SecurityAuditEventType.PUBLIC_REQUEST_UNCLASSIFIED;
        return classify(operation, status);
    }

    private SecurityAuditEventType classify(AuditedOperation operation, int status) {
        if (status == 429)
            return switch (operation) {
                case LOGIN -> SecurityAuditEventType.LOGIN_RATE_LIMITED;
                case USER_REGISTRATION -> SecurityAuditEventType.USER_REGISTRATION_RATE_LIMITED;
            };
        if (status >= HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
            return switch (operation) {
                case LOGIN -> SecurityAuditEventType.LOGIN_UNAVAILABLE;
                case USER_REGISTRATION -> SecurityAuditEventType.USER_REGISTRATION_UNAVAILABLE;
            };
        return switch (operation) {
            case LOGIN -> classifyLoginStatus(status);
            case USER_REGISTRATION -> classifyRegistrationStatus(status);
        };
    }

    private SecurityAuditEventType classifyLoginStatus(int status) {
        return switch (status) {
            case HttpServletResponse.SC_OK -> SecurityAuditEventType.LOGIN_SUCCEEDED;
            case HttpServletResponse.SC_UNAUTHORIZED -> SecurityAuditEventType.LOGIN_FAILED;
            default -> SecurityAuditEventType.LOGIN_REJECTED;
        };
    }

    private SecurityAuditEventType classifyRegistrationStatus(int status) {
        return switch (status) {
            case HttpServletResponse.SC_CREATED -> SecurityAuditEventType.USER_REGISTRATION_SUCCEEDED;
            default -> SecurityAuditEventType.USER_REGISTRATION_REJECTED;
        };
    }

    private enum AuditedOperation {
        LOGIN,
        USER_REGISTRATION;

        private static AuditedOperation fromPath(String path) {
            return switch (path) {
                case LOGIN_PATH -> LOGIN;
                case USER_CREATE_PATH, USER_CREATE_PATH + "/" -> USER_REGISTRATION;
                default -> null;
            };
        }
    }
    
    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), Map.of(
                "timestamp", Instant.now().toString(),
                "status", status,
                "error", HttpStatus.valueOf(status).getReasonPhrase(),
                "message", message));
    }

    private static final class CachedBodyRequest extends HttpServletRequestWrapper {
        private final byte[] body;

        private CachedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return input.read();
                }

                @Override
                public boolean isFinished() {
                    return input.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    try {
                        if (input.available() > 0)
                            listener.onDataAvailable();
                        if (input.available() == 0)
                            listener.onAllDataRead();
                    } catch (IOException exception) {
                        listener.onError(exception);
                    }
                }
            };
        }
    }
}
