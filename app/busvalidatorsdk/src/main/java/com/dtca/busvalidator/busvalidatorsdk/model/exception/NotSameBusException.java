package com.dtca.busvalidator.busvalidatorsdk.model.exception;


public class NotSameBusException extends Exception {

    public NotSameBusException() {
    }

    public NotSameBusException(String message) {
        super(message);
    }

    public NotSameBusException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotSameBusException(Throwable cause) {
        super(cause);
    }
}
