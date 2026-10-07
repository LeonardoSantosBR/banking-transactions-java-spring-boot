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
import leonardo.banking_transactions.exceptions.RateLimitExceededException;
import leonardo.banking_transactions.services.RateLimitService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String USER_CREATE_PATH = "/api/users";
    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(RateLimitService rateLimitService, ObjectMapper objectMapper) {
        this.rateLimitService = rateLimitService;
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
        byte[] body = request.getInputStream().readNBytes(8193);
        if (body.length > 8192) {
            writeError(response, 413, "Request body is too large.");
            return;
        }
        String cpf = "";
        try {
            JsonNode node = objectMapper.readTree(body);
            if (node != null && node.hasNonNull("cpf"))
                cpf = node.get("cpf").asText("");
        } catch (JsonProcessingException ignored) {
            // Let MVC return its normal malformed-request response after limiting by IP.
        }
        try {
            if (LOGIN_PATH.equals(request.getServletPath()))
                rateLimitService.checkAndConsumeLogin(request.getRemoteAddr(), cpf);
            else
                rateLimitService.checkAndConsumeUserCreation(request.getRemoteAddr(), cpf);
        } catch (RateLimitExceededException exception) {
            writeError(response, 429, "Too many requests. Try again later.");
            return;
        } catch (RuntimeException exception) {
            writeError(response, 503, "Request processing is temporarily unavailable.");
            return;
        }

        chain.doFilter(new CachedBodyRequest(request, body), response);
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
                @Override public int read() { return input.read(); }
                @Override public boolean isFinished() { return input.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener listener) {
                    try {
                        if (input.available() > 0) listener.onDataAvailable();
                        if (input.available() == 0) listener.onAllDataRead();
                    } catch (IOException exception) {
                        listener.onError(exception);
                    }
                }
            };
        }
    }
}
