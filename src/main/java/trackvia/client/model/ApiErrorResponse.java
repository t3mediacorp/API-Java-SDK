package trackvia.client.model;

import java.util.List;

/**
 * Captures the two ways the Trackvia Service passes back error information, as name/value pairs:
 *
 * 1. error + error_description (optional)
 * 2. errors + message + name + code + stackTrace (optional)
 */
public class ApiErrorResponse {
    // Type 1 error response
    private List<String> errors;
    private String message;
    private String name;
    private String code;
    private String stackTrace;

    // Type 2 error response
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

    public List<String> getErrors() { return errors; }

    public void setErrors(List<String> errors) { this.errors = errors; }

    public String getMessage() { return message; }

    public void setMessage(String message) { this.message = message; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }

    public void setCode(String code) { this.code = code; }

    public String getStackTrace() { return stackTrace; }

    public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }
}
