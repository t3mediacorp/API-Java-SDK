package trackvia.client.model;

public class ApiErrorResponse {
    private String error;
    private String error_description;

    public ApiErrorResponse() {}

    public ApiError getApiError() {
        return ApiError.get(error);
    }

    private String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    private String getError_description() {
        return error_description;
    }

    public void setError_description(String error_description) {
        this.error_description = error_description;
    }
}
