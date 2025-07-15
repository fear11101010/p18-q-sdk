package com.dtca.busvalidator.busvalidatorsdk.model.exception;


public class CancelOfEntryNotAllowedException extends Exception {

    public CancelOfEntryNotAllowedException() {
    }

    public CancelOfEntryNotAllowedException(String message) {
        super(message);
    }

    public CancelOfEntryNotAllowedException(String message, Throwable cause) {
        super(message, cause);
    }

    public CancelOfEntryNotAllowedException(Throwable cause) {
        super(cause);
    }
}
