package trackvia.client;

import trackvia.client.model.ApiError;
import trackvia.client.model.ApiErrorResponse;

public class TrackviaApiException extends RuntimeException {
    private ApiErrorResponse apiErrorResponse;
    private Throwable cause;

    public TrackviaApiException(final ApiErrorResponse apiErrorResponse) {
    	super(apiErrorResponse.getMessage());
        this.apiErrorResponse = apiErrorResponse;
        this.cause = null;
    }

    public TrackviaApiException(final ApiErrorResponse apiErrorResponse, final Throwable cause) {
        this.apiErrorResponse = apiErrorResponse;
        this.cause = cause;
    }

    public ApiError getApiError() {
        return apiErrorResponse.getApiError();
    }

    public void setApiErrorResponse(ApiErrorResponse apiErrorResponse) {
        this.apiErrorResponse = apiErrorResponse;
    }

    public Throwable getCause() {
        return cause;
    }

    public void setCause(Throwable cause) {
        this.cause = cause;
    }
}
