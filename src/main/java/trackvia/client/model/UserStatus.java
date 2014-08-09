package trackvia.client.model;

import java.util.HashMap;
import java.util.Map;

public enum UserStatus {
    Active("ACTIVE"),
    Unverified("UNVERIFIED");

    private static Map<String, UserStatus> lookupTable = new HashMap<String, UserStatus>();

    static {
        for (UserStatus status : values()) {
            lookupTable.put(status.code, status);
        }
    }

    private String code;

    UserStatus(final String code) {
        this.code = code;
    }

    public String code() { return this.code; }

    public static UserStatus get(final String code) {
        return lookupTable.get(code);
    }
}
