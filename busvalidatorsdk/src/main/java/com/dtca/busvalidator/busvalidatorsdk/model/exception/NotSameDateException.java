package com.dtca.busvalidator.busvalidatorsdk.model.exception;


public class NotSameDateException extends Exception {

    public NotSameDateException() {
    }

    public NotSameDateException(String message) {
        super(message);
    }

    public NotSameDateException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotSameDateException(Throwable cause) {
        super(cause);
    }
}
