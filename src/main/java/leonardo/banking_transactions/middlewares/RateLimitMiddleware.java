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
import leonardo.banking_transactions.config.RateLimitOperationConfig;
import leonardo.banking_transactions.enums.SecurityAuditEventTypeEnum;
import leonardo.banking_transactions.exceptions.RateLimitExceededException;
import leonardo.banking_transactions.services.RateLimitService;
import leonardo.banking_transactions.services.SecurityAuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@Component
public class RateLimitMiddleware extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(RateLimitMiddleware.class);
    private static final int MAX_BODY_BYTES = 8192;
    private static final int MAX_CPF_LENGTH = 32;
    private static final String UNAVAILABLE_MESSAGE = "Request processing is temporarily unavailable.";
    private final RateLimitService rateLimitService;
    private final SecurityAuditService securityAuditService;
    private final ObjectMapper objectMapper;

    public RateLimitMiddleware(
            RateLimitService rateLimitService,
            SecurityAuditService securityAuditService,
            ObjectMapper objectMapper) {
        this.rateLimitService = rateLimitService;
        this.securityAuditService = securityAuditService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod())
                || RateLimitOperationConfig.fromPath(request.getServletPath()) == null;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        RateLimitOperationConfig operation = RateLimitOperationConfig.fromPath(request.getServletPath());
        String cpf = "";
        byte[] body = request.getInputStream().readNBytes(MAX_BODY_BYTES + 1);
        if (body.length > MAX_BODY_BYTES) {
            if (!recordEvent(request, cpf, eventType(operation, 413))) {
                writeError(response, 503, UNAVAILABLE_MESSAGE);
                return;
            }
            writeError(response, 413, "Request body is too large.");
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(body);
            if (node != null && node.hasNonNull("cpf")) {
                String rawCpf = node.get("cpf").asText("");
                cpf = rawCpf.length() > MAX_CPF_LENGTH ? rawCpf.substring(0, MAX_CPF_LENGTH) : rawCpf;
            }
        } catch (JsonProcessingException ignored) {
        }
        try {
            rateLimitService.checkAndConsume(operation, request.getRemoteAddr(), cpf);
        } catch (RateLimitExceededException exception) {
            if (!recordEvent(request, cpf, eventType(operation, 429))) {
                writeError(response, 503, UNAVAILABLE_MESSAGE);
                return;
            }
            writeError(response, 429, "Too many requests. Try again later.");
            return;
        } catch (RuntimeException exception) {
            recordEvent(request, cpf, eventType(operation, 503));
            writeError(response, 503, UNAVAILABLE_MESSAGE);
            return;
        }

        ContentCachingResponseWrapper bufferedResponse = new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(new CachedBodyRequest(request, body), bufferedResponse);
        } catch (IOException | ServletException | RuntimeException exception) {
            recordEvent(request, cpf, eventType(operation, 503));
            throw exception;
        }

        SecurityAuditEventTypeEnum responseEventType = eventType(operation, bufferedResponse.getStatus());
        try {
            securityAuditService.record(
                    responseEventType,
                    cpf,
                    request.getRemoteAddr(),
                    request.getHeader("User-Agent"));
        } catch (RuntimeException exception) {
            logger.error("Could not persist security audit event for a public endpoint");
            if (responseEventType != SecurityAuditEventTypeEnum.USER_REGISTRATION_SUCCEEDED) {
                response.reset();
                writeError(response, 503, UNAVAILABLE_MESSAGE);
                return;
            }
        }
        bufferedResponse.copyBodyToResponse();
    }

    private boolean recordEvent(
            HttpServletRequest request,
            String cpf,
            SecurityAuditEventTypeEnum eventType) {
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

    private SecurityAuditEventTypeEnum eventType(RateLimitOperationConfig operation, int status) {
        if (status == 429)
            return switch (operation) {
                case LOGIN -> SecurityAuditEventTypeEnum.LOGIN_RATE_LIMITED;
                case CREATE_USER -> SecurityAuditEventTypeEnum.USER_REGISTRATION_RATE_LIMITED;
            };
        if (status >= HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
            return switch (operation) {
                case LOGIN -> SecurityAuditEventTypeEnum.LOGIN_UNAVAILABLE;
                case CREATE_USER -> SecurityAuditEventTypeEnum.USER_REGISTRATION_UNAVAILABLE;
            };
        return switch (operation) {
            case LOGIN -> classifyLoginStatus(status);
            case CREATE_USER -> classifyRegistrationStatus(status);
        };
    }

    private SecurityAuditEventTypeEnum classifyLoginStatus(int status) {
        return switch (status) {
            case HttpServletResponse.SC_OK -> SecurityAuditEventTypeEnum.LOGIN_SUCCEEDED;
            case HttpServletResponse.SC_UNAUTHORIZED -> SecurityAuditEventTypeEnum.LOGIN_FAILED;
            default -> SecurityAuditEventTypeEnum.LOGIN_REJECTED;
        };
    }

    private SecurityAuditEventTypeEnum classifyRegistrationStatus(int status) {
        return status == HttpServletResponse.SC_CREATED
                ? SecurityAuditEventTypeEnum.USER_REGISTRATION_SUCCEEDED
                : SecurityAuditEventTypeEnum.USER_REGISTRATION_REJECTED;
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