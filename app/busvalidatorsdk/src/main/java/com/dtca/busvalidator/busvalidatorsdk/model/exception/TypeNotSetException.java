package com.dtca.busvalidator.busvalidatorsdk.model.exception;


public class TypeNotSetException extends Exception {

    public TypeNotSetException() {
    }

    public TypeNotSetException(String message) {
        super(message);
    }

    public TypeNotSetException(String message, Throwable cause) {
        super(message, cause);
    }

    public TypeNotSetException(Throwable cause) {
        super(cause);
    }
}
