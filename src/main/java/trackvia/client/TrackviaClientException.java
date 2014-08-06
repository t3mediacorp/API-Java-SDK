package trackvia.client;

public class TrackviaClientException extends RuntimeException {
    private String message;
    private Throwable cause;

    public TrackviaClientException(final String message) {
        this.message = message;
    }

    public TrackviaClientException(final Throwable cause) {
        this.cause = cause;
    }

    @Override
    public Throwable getCause() {
        return cause;
    }

    public void setCause(Throwable cause) {
        this.cause = cause;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
