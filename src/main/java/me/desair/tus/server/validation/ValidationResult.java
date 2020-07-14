package me.desair.tus.server.validation;

public class ValidationResult {
    private boolean failure;
    private String errorMessage;
    private int httpStatus;

    /**
     * Creates {@link ValidationResult} with no error
     */
    public ValidationResult(){
        this.failure = false;
    }

    /**
     * Creates a {@link ValidationResult} with an error
     * @param status http status code to be returned
     * @param errorMessage error message
     */
    public ValidationResult(int status, String errorMessage) {
        this.httpStatus = status;
        this.errorMessage = errorMessage;
        this.failure = true;
    }

    public boolean isFailure() {
        return failure;
    }
}
