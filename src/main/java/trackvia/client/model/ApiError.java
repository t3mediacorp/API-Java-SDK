package trackvia.client.model;

import java.util.HashMap;
import java.util.Map;

public enum ApiError {
    InvalidGrant("invalid_grant", "Invalid authorization grant"),
    InvalidToken("invalid_token", "Invalid access or refresh token"),
	VersionMisMatch("version_mismatch", "requested version and api version don't match");

    private static Map<String, ApiError> lookupTable = new HashMap<String, ApiError>();

    static {
        for (ApiError error : values()) {
            lookupTable.put(error.code, error);
        }
    }

    private String code;
    private String description;

    ApiError(final String code, final String description) {
        this.code = code;
        this.description = description;
    }

    public String code() { return this.code; }

    public String description() { return this.description; }

    public static ApiError get(final String code) {
        return lookupTable.get(code);
    }
}
