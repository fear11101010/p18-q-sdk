package com.dtca.busvalidator.busvalidatorsdk.model.exception;


public class CardWriteException extends Exception {

    public CardWriteException() {
    }

    public CardWriteException(String message) {
        super(message);
    }

    public CardWriteException(String message, Throwable cause) {
        super(message, cause);
    }

    public CardWriteException(Throwable cause) {
        super(cause);
    }
}
