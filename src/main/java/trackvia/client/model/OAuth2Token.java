package trackvia.client.model;

import java.util.Date;

public class OAuth2Token {
    public enum Type { bearer }

    private String value;
    private Type tokenType;
    private RefreshToken refreshToken;
    private Long expires_in;
    private Long expiresIn;
    private Date expiration;
    private String[] scope;
    private String access_token;
    private String accessToken;
    private String refresh_token;
    private String apiVersion;

    public OAuth2Token() {}

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Type getTokenType() {
        return tokenType;
    }

    public void setTokenType(Type tokenType) {
        this.tokenType = tokenType;
    }

    public RefreshToken getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(RefreshToken refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Long getExpires_in() {
        return expires_in;
    }

    public void setExpires_in(Long expires_in) {
        this.expires_in = expires_in;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public Date getExpiration() {
        return expiration;
    }

    public void setExpiration(Date expiration) {
        this.expiration = expiration;
    }

    public String[] getScope() {
        return scope;
    }

    public void setScope(String[] scope) {
        this.scope = scope;
    }

    public String getAccess_token() {
        return access_token;
    }

    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefresh_token() {
        return refresh_token;
    }

    public void setRefresh_token(String refresh_token) {
        this.refresh_token = refresh_token;
    }
    
    

    public String getApiVersion() {
		return apiVersion;
	}

	public void setApiVersion(String apiVersion) {
		this.apiVersion = apiVersion;
	}

	@Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (!(o instanceof OAuth2Token)) return false;

        OAuth2Token otherToken = (OAuth2Token) o;

        if (otherToken.value == null || value == null) return false;
        if (!otherToken.value.equals(value)) return false;
        if (otherToken.refreshToken == null || refreshToken == null) return false;
        if (!otherToken.refreshToken.equals(refreshToken)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) ((value != null) ? (value.hashCode()) : (0));
        result = prime * result + (int) ((refreshToken != null) ? (refreshToken.hashCode()) : (0));

        return result;
    }

    public static class RefreshToken {
        private String value;
        private Date expiration;

        public RefreshToken() {}

        public RefreshToken(final String value, final Date expiration) {
            this.value = value;
            this.expiration = expiration;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public Date getExpiration() {
            return expiration;
        }

        public void setExpiration(Date expiration) {
            this.expiration = expiration;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) return false;
            if (!(o instanceof RefreshToken)) return false;

            RefreshToken otherToken = (RefreshToken) o;

            if (otherToken.value == null || value == null) return false;
            if (!otherToken.value.equals(value)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) ((value != null) ? (value.hashCode()) : (0));

            return result;
        }
    }
}
