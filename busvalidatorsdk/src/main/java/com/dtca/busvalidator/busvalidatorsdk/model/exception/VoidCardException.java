package com.dtca.busvalidator.busvalidatorsdk.model.exception;


public class VoidCardException extends Exception {

    public VoidCardException() {
    }

    public VoidCardException(String message) {
        super(message);
    }

    public VoidCardException(String message, Throwable cause) {
        super(message, cause);
    }

    public VoidCardException(Throwable cause) {
        super(cause);
    }
}
