package leonardo.banking_transactions.config;

/* It is a list of rules by operation
  - endpoint
  - bucket scope name
  - limit by IP
  - limit by CPF
*/
public enum RateLimitOperationConfig {
    LOGIN("/api/auth/login", "login", 30, 5),
    CREATE_USER("/api/users", "create_user", 10, 3);

    private final String path;
    private final String bucketScope;
    private final long ipLimit;
    private final long cpfLimit;

    RateLimitOperationConfig(String path, String bucketScope, long ipLimit, long cpfLimit) {
        this.path = path;
        this.bucketScope = bucketScope;
        this.ipLimit = ipLimit;
        this.cpfLimit = cpfLimit;
    }

    public String path() {
        return path;
    }

    public String bucketScope() {
        return bucketScope;
    }

    public long ipLimit() {
        return ipLimit;
    }

    public long cpfLimit() {
        return cpfLimit;
    }

    public static RateLimitOperationConfig fromPath(String rawPath) {
        if (rawPath == null)
            return null;
        String normalized = rawPath.length() > 1 && rawPath.endsWith("/")
                ? rawPath.substring(0, rawPath.length() - 1)
                : rawPath;
        for (RateLimitOperationConfig operation : values())
            if (operation.path.equals(normalized))
                return operation;
        return null;
    }
}