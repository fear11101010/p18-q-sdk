package com.dtca.busvalidator.busvalidatorsdk.model.exception;


public class NotBusException extends Exception {

    public NotBusException() {
    }

    public NotBusException(String message) {
        super(message);
    }

    public NotBusException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotBusException(Throwable cause) {
        super(cause);
    }
}
