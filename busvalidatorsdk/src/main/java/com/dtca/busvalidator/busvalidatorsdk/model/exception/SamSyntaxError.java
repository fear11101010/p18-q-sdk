package com.dtca.busvalidator.busvalidatorsdk.model.exception;

public class SamSyntaxError extends Exception{
    public SamSyntaxError() {
        super();
    }

    public SamSyntaxError(String message) {
        super(message);
    }

    public SamSyntaxError(String message, Throwable cause) {
        super(message, cause);
    }

    public SamSyntaxError(Throwable cause) {
        super(cause);
    }
}
